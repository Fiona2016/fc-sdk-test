# 网络追踪使用指南

## 已添加的文件

### 1. `app/src/main/java/com/example/fc_sdk_test/network/NetworkManager.kt`
集中式网络配置管理器，提供预配置的 OkHttpClient，已集成 DatadogInterceptor。

**核心配置**:
```kotlin
import cloud.flashcat.android.okhttp.DatadogInterceptor
import cloud.flashcat.android.okhttp.TracingHeaderType

private val tracedHostsWithHeaderType = mapOf(
    "httpbin.org" to setOf(
        TracingHeaderType.DATADOG,
        TracingHeaderType.TRACECONTEXT
    ),
    "api.github.com" to setOf(
        TracingHeaderType.DATADOG,
        TracingHeaderType.TRACECONTEXT
    ),
    "jsonplaceholder.typicode.com" to setOf(
        TracingHeaderType.DATADOG,
        TracingHeaderType.TRACECONTEXT
    )
)

val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(
        DatadogInterceptor.Builder(tracedHostsWithHeaderType).build()
    )
    .build()
```

### 2. `app/src/main/java/com/example/fc_sdk_test/network/ApiService.kt`
网络请求服务类，提供常用的 HTTP 请求方法和测试端点。

### 3. `app/src/main/java/com/example/fc_sdk_test/ui/slideshow/SlideshowFragment.kt`
已更新，添加了网络追踪演示功能。

## 快速使用

### 方式 1: 使用 ApiService（推荐）

```kotlin
import com.example.fc_sdk_test.network.ApiService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

val apiService = ApiService()

lifecycleScope.launch {
    val result = apiService.testHttpBin()
    result.onSuccess { response ->
        // 处理响应
        Log.d(TAG, "Success: $response")
    }.onFailure { error ->
        // 处理错误
        Log.e(TAG, "Failed: ${error.message}")
    }
}
```

### 方式 2: 直接使用 NetworkManager

```kotlin
import com.example.fc_sdk_test.network.NetworkManager
import okhttp3.Request

val client = NetworkManager.okHttpClient

val request = Request.Builder()
    .url("https://httpbin.org/get")
    .build()

// 在协程中执行
val response = client.newCall(request).execute()
```

### 方式 3: 与 Retrofit 集成

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.fc_sdk_test.network.NetworkManager

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(NetworkManager.okHttpClient)  // 使用配置好的 client
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(YourApiInterface::class.java)
```

## 追踪头类型

- **TracingHeaderType.DATADOG**: Datadog 专有格式
- **TracingHeaderType.TRACECONTEXT**: W3C Trace Context 标准格式

## 添加自定义主机

编辑 `NetworkManager.kt` 中的 `tracedHostsWithHeaderType`:

```kotlin
private val tracedHostsWithHeaderType = mapOf(
    "your-api.example.com" to setOf(
        TracingHeaderType.DATADOG,
        TracingHeaderType.TRACECONTEXT
    )
)
```

## 验证追踪功能

### 1. 运行应用
- 导航到 "Slideshow" 页面
- 查看 Logcat 中的日志

### 2. 预期日志输出
```
D/ApiService: Making GET request to: https://httpbin.org/get
D/ApiService: Request successful. Response length: ...
D/SlideshowFragment: ✓ Network request successful with tracing
```

### 3. 查看追踪数据
登录 Flashcat/Datadog 控制台，在 APM > Traces 部分查看完整的请求追踪链路。

## 关键点

✅ 使用 `DatadogInterceptor` 而不是 `FlashcatInterceptor`  
✅ `TracingHeaderType` 从 `cloud.flashcat.android.okhttp` 直接导入  
✅ 使用 `TracingHeaderType.DATADOG` 和 `TracingHeaderType.TRACECONTEXT`  
✅ 所有请求自动包含追踪头，无需手动添加  
✅ 支持协程异步请求  
✅ 完整的错误处理

## 示例代码

完整示例请查看 `SlideshowFragment.kt` 中的 `demonstrateNetworkTracing()` 方法。
