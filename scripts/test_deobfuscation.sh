#!/bin/bash

# 反混淆测试脚本
# 用于验证 Datadog 的反混淆功能是否正常工作

echo "=========================================="
echo "Datadog 反混淆测试脚本"
echo "=========================================="
echo ""

# 1. 检查 mapping 文件是否存在
MAPPING_FILE="app/build/outputs/mapping/release/mapping.txt"
if [ -f "$MAPPING_FILE" ]; then
    echo "✓ Mapping 文件存在: $MAPPING_FILE"
    echo "  文件大小: $(du -h "$MAPPING_FILE" | cut -f1)"
    echo "  行数: $(wc -l < "$MAPPING_FILE")"
else
    echo "✗ Mapping 文件不存在: $MAPPING_FILE"
    echo "  请先构建 release 版本: ./gradlew assembleRelease"
    exit 1
fi

echo ""

# 2. 检查是否包含 ReflowFragment 的映射
if grep -q "ReflowFragment" "$MAPPING_FILE"; then
    echo "✓ Mapping 文件中包含 ReflowFragment 的映射"
    echo ""
    echo "  相关映射示例:"
    grep -i "reflowfragment" "$MAPPING_FILE" | head -5
else
    echo "⚠ Mapping 文件中未找到 ReflowFragment"
    echo "  这可能意味着该类未被混淆（正常情况）"
fi

echo ""

# 3. 检查 lambda 相关的映射
if grep -q "\$lambda" "$MAPPING_FILE"; then
    echo "✓ Mapping 文件中包含 lambda 相关的映射"
    echo ""
    echo "  Lambda 映射示例:"
    grep "\$lambda" "$MAPPING_FILE" | head -5
else
    echo "⚠ Mapping 文件中未找到 lambda 映射"
fi

echo ""

# 4. 提示上传 mapping 文件
echo "=========================================="
echo "下一步操作:"
echo "=========================================="
echo ""
echo "1. 上传 mapping 文件到 Datadog:"
echo "   ./gradlew uploadMappingRelease"
echo ""
echo "2. 确保版本号匹配:"
echo "   - build.gradle.kts 中的 versionName"
echo "   - datadog 配置块中的 versionName"
echo ""
echo "3. 在应用中触发错误，然后:"
echo "   - 检查 Datadog 控制台中的错误堆栈"
echo "   - 对比 Logcat 中的混淆堆栈"
echo "   - 验证是否已正确反混淆"
echo ""
echo "4. 使用 retrace 工具本地测试:"
echo "   \$ANDROID_HOME/tools/proguard/bin/retrace.sh \\"
echo "     $MAPPING_FILE \\"
echo "     obfuscated_stacktrace.txt"
echo ""
