package com.example.fc_sdk_test.util

import android.util.Log

/**
 * 混淆测试辅助类
 * 
 * 这个类会被混淆，因为：
 * 1. 它不在 AndroidManifest.xml 中声明
 * 2. 它不是 Activity、Fragment、Service 等系统组件
 * 3. 它是普通的业务逻辑类
 * 
 * 用途：测试混淆和反混淆功能
 */
class ObfuscationTestHelper {
    
    companion object {
        private const val TAG = "ObfuscationTestHelper"
    }
    
    /**
     * 测试方法 1: 简单的业务逻辑方法
     * 这个方法会被混淆，方法名会变成 a、b、c 等单字母
     */
    fun processUserData(userId: String): String {
        Log.d(TAG, "Processing user data for: $userId")
        return validateAndProcess(userId)
    }
    
    /**
     * 测试方法 2: 私有方法
     * 这个方法会被混淆，且方法名会更短
     */
    private fun validateAndProcess(userId: String): String {
        if (userId.isEmpty()) {
            throw IllegalArgumentException("User ID cannot be empty")
        }
        return "Processed: $userId"
    }
    
    /**
     * 测试方法 3: 嵌套调用，创建深层堆栈跟踪
     * 用于测试反混淆是否能正确处理多层调用
     */
    fun createDeepStackTrace(): RuntimeException {
        return level1()
    }
    
    private fun level1(): RuntimeException {
        return level2()
    }
    
    private fun level2(): RuntimeException {
        return level3()
    }
    
    private fun level3(): RuntimeException {
        return level4()
    }
    
    private fun level4(): RuntimeException {
        return RuntimeException("Deep stack trace test - Level 4")
    }
    
    /**
     * 测试方法 4: Lambda 表达式测试
     * Lambda 会被转换为合成方法，用于测试 lambda 反混淆
     */
    fun testLambdaExpressions(): String {
        val lambda1: (String) -> String = { input ->
            val lambda2: (String) -> String = { nestedInput ->
                if (nestedInput.isEmpty()) {
                    throw IllegalArgumentException("Lambda test error: empty input")
                }
                "Lambda processed: $nestedInput"
            }
            lambda2(input)
        }
        
        return lambda1("test")
    }
    
    /**
     * 测试方法 5: 抛出异常用于测试堆栈跟踪
     */
    fun throwTestException(message: String): Nothing {
        throw TestException(message)
    }
    
    /**
     * 自定义异常类
     * 这个类也会被混淆
     */
    class TestException(message: String) : RuntimeException(message) {
        init {
            Log.e(TAG, "TestException created: $message")
        }
    }
    
    /**
     * 测试方法 6: 复杂的数据处理
     * 包含多个方法调用，用于测试完整的调用链反混淆
     */
    fun complexDataProcessing(data: Map<String, Any>): String {
        val validated = validateData(data)
        val processed = processData(validated)
        return formatResult(processed)
    }
    
    private fun validateData(data: Map<String, Any>): Map<String, Any> {
        if (data.isEmpty()) {
            throw IllegalArgumentException("Data cannot be empty")
        }
        return data
    }
    
    private fun processData(data: Map<String, Any>): List<String> {
        return data.keys.map { key ->
            transformKey(key)
        }
    }
    
    private fun transformKey(key: String): String {
        return key.uppercase()
    }
    
    private fun formatResult(result: List<String>): String {
        return result.joinToString(", ")
    }
}
