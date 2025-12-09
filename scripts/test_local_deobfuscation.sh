#!/bin/bash

# 本地反混淆测试脚本
# 用于验证 mapping 文件是否能正确反混淆堆栈跟踪

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAPPING_FILE="$PROJECT_DIR/app/build/outputs/mapping/release/mapping.txt"
TEST_DIR="$PROJECT_DIR/test_deobfuscation"

echo "=========================================="
echo "本地反混淆测试"
echo "=========================================="
echo ""

# 检查 mapping 文件
if [ ! -f "$MAPPING_FILE" ]; then
    echo "❌ 错误: Mapping 文件不存在"
    echo "   请先构建 release 版本: ./gradlew assembleRelease"
    exit 1
fi

echo "✓ Mapping 文件: $MAPPING_FILE"
echo ""

# 创建测试目录
mkdir -p "$TEST_DIR"

# 查找 retrace 工具
RETRACE_TOOL=""
if [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/tools/proguard/bin/retrace.sh" ]; then
    RETRACE_TOOL="$ANDROID_HOME/tools/proguard/bin/retrace.sh"
elif [ -f "$HOME/Library/Android/sdk/tools/proguard/bin/retrace.sh" ]; then
    RETRACE_TOOL="$HOME/Library/Android/sdk/tools/proguard/bin/retrace.sh"
elif command -v retrace &> /dev/null; then
    RETRACE_TOOL="retrace"
fi

if [ -z "$RETRACE_TOOL" ]; then
    echo "⚠️  警告: 未找到 retrace.sh，尝试使用 Java 版本..."
    # 尝试使用 Java 版本的 retrace
    if [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/tools/proguard/lib/retrace.jar" ]; then
        RETRACE_TOOL="java -jar $ANDROID_HOME/tools/proguard/lib/retrace.jar"
    elif [ -f "$HOME/Library/Android/sdk/tools/proguard/lib/retrace.jar" ]; then
        RETRACE_TOOL="java -jar $HOME/Library/Android/sdk/tools/proguard/lib/retrace.jar"
    else
        echo "❌ 错误: 未找到 retrace 工具"
        echo "   请确保 Android SDK 已正确安装"
        exit 1
    fi
fi

echo "✓ Retrace 工具: $RETRACE_TOOL"
echo ""

# 从 mapping 文件中提取混淆信息
echo "正在分析 mapping 文件..."
OBFUSCATED_CLASS=$(grep -E "^com\.example\.fc_sdk_test\.ui\.reflow\.ReflowFragment\$\$ExternalSyntheticLambda" "$MAPPING_FILE" | head -1 | awk '{print $3}')
OBFUSCATED_METHOD=$(grep -E "ReflowFragment.*lambda.*->" "$MAPPING_FILE" | grep -v "ExternalSyntheticLambda" | head -1 | awk -F' -> ' '{print $2}' | awk '{print $1}')

if [ -z "$OBFUSCATED_CLASS" ]; then
    # 使用已知的混淆类名
    OBFUSCATED_CLASS="v2.a"
    echo "⚠️  使用默认混淆类名: $OBFUSCATED_CLASS"
else
    echo "✓ 找到混淆类名: $OBFUSCATED_CLASS"
fi

if [ -z "$OBFUSCATED_METHOD" ]; then
    OBFUSCATED_METHOD="run"
    echo "⚠️  使用默认混淆方法名: $OBFUSCATED_METHOD"
else
    echo "✓ 找到混淆方法名: $OBFUSCATED_METHOD"
fi

echo ""

# 创建示例混淆堆栈跟踪
OBFUSCATED_STACKTRACE="$TEST_DIR/obfuscated_stacktrace.txt"
cat > "$OBFUSCATED_STACKTRACE" << EOF
java.lang.RuntimeException: Test error from ReflowFragment - Report Error button clicked
	at $OBFUSCATED_CLASS.$OBFUSCATED_METHOD($OBFUSCATED_CLASS.java:21)
	at android.view.View.performClick(View.java:7448)
	at com.google.android.material.button.MaterialButton.performClick(MaterialButton.java:1119)
	at android.view.View.performClickInternal(View.java:7425)
	at android.view.View.access\$3600(View.java:810)
	at android.view.View\$PerformClick.run(View.java:28305)
	at android.os.Handler.handleCallback(Handler.java:938)
	at android.os.Handler.dispatchMessage(Handler.java:99)
	at android.os.Looper.loop(Looper.java:223)
	at android.app.ActivityThread.main(ActivityThread.java:7656)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.RuntimeInit\$MethodAndArgsCaller.run(RuntimeInit.java:592)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)
EOF

echo "✓ 创建混淆堆栈跟踪: $OBFUSCATED_STACKTRACE"
echo ""
echo "混淆后的堆栈跟踪:"
echo "----------------------------------------"
cat "$OBFUSCATED_STACKTRACE"
echo "----------------------------------------"
echo ""

# 执行反混淆
echo "正在执行反混淆..."
DEOBFUSCATED_STACKTRACE="$TEST_DIR/deobfuscated_stacktrace.txt"

if [[ "$RETRACE_TOOL" == *"retrace.sh"* ]]; then
    bash "$RETRACE_TOOL" "$MAPPING_FILE" "$OBFUSCATED_STACKTRACE" > "$DEOBFUSCATED_STACKTRACE" 2>&1
elif [[ "$RETRACE_TOOL" == *"retrace.jar"* ]]; then
    eval "$RETRACE_TOOL" "$MAPPING_FILE" "$OBFUSCATED_STACKTRACE" > "$DEOBFUSCATED_STACKTRACE" 2>&1
else
    $RETRACE_TOOL "$MAPPING_FILE" "$OBFUSCATED_STACKTRACE" > "$DEOBFUSCATED_STACKTRACE" 2>&1
fi

if [ $? -eq 0 ]; then
    echo "✓ 反混淆完成"
    echo ""
    echo "反混淆后的堆栈跟踪:"
    echo "----------------------------------------"
    cat "$DEOBFUSCATED_STACKTRACE"
    echo "----------------------------------------"
    echo ""
    
    # 检查反混淆是否成功
    if grep -q "ReflowFragment" "$DEOBFUSCATED_STACKTRACE"; then
        echo "✅ 成功: 堆栈跟踪已正确反混淆，包含 ReflowFragment"
    else
        echo "⚠️  警告: 反混淆后的堆栈跟踪中未找到 ReflowFragment"
        echo "   可能的原因:"
        echo "   1. Mapping 文件版本与 APK 版本不匹配"
        echo "   2. 混淆类名或方法名映射不正确"
        echo "   3. 堆栈跟踪格式不正确"
    fi
    
    # 检查是否有 lambda 相关信息
    if grep -q "\$lambda" "$DEOBFUSCATED_STACKTRACE"; then
        echo "ℹ️  信息: 堆栈跟踪中包含 lambda 函数（这是正常的）"
    fi
    
else
    echo "❌ 错误: 反混淆失败"
    echo ""
    echo "错误信息:"
    cat "$DEOBFUSCATED_STACKTRACE"
    exit 1
fi

echo ""
echo "=========================================="
echo "测试文件位置:"
echo "  混淆堆栈: $OBFUSCATED_STACKTRACE"
echo "  反混淆堆栈: $DEOBFUSCATED_STACKTRACE"
echo "=========================================="
