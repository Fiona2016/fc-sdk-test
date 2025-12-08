# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================
# 测试反混淆：允许混淆 Activity 和 Fragment
# ============================================
# 注意：这会允许混淆 Activity 和 Fragment，仅用于测试反混淆功能
# 在生产环境中，通常需要保留这些类以便通过反射访问

# 允许混淆 MainActivity（仅用于测试）
# -keep class com.example.fc_sdk_test.MainActivity { *; }

# 允许混淆 ReflowFragment（仅用于测试）
# -keep class com.example.fc_sdk_test.ui.reflow.ReflowFragment { *; }

# 默认情况下，Android 的 ProGuard 规则会保留所有 Activity 和 Fragment
# 因为它们在 AndroidManifest.xml 中声明，且可能通过反射访问
# 如果要测试混淆，可以注释掉默认规则，但需要确保应用仍能正常运行