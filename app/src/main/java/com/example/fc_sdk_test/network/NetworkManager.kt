package com.example.fc_sdk_test.network

import com.datadog.android.okhttp.DatadogInterceptor
import com.datadog.android.trace.TracingHeaderType
import com.example.fc_sdk_test.flow.FlowResourceAttributesProvider
import com.example.fc_sdk_test.flow.FlowTaggingInterceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Centralized network configuration manager
 * Provides pre-configured OkHttpClient with Datadog tracing
 */
object NetworkManager {
    
    // Configure traced hosts with header types
    private val tracedHostsWithHeaderType = mapOf(
        "httpbin.org" to setOf(
            TracingHeaderType.B3
        ),
        "api.github.com" to setOf(
            TracingHeaderType.DATADOG
        ),
        "jsonplaceholder.typicode.com" to setOf(
            TracingHeaderType.TRACECONTEXT
        ),
        // badssl.com for SSL certificate failure testing
        "expired.badssl.com" to setOf(TracingHeaderType.DATADOG),
        "wrong.host.badssl.com" to setOf(TracingHeaderType.DATADOG),
        "self-signed.badssl.com" to setOf(TracingHeaderType.DATADOG),
        "untrusted-root.badssl.com" to setOf(TracingHeaderType.DATADOG)
    )
    
    /**
     * Pre-configured OkHttpClient with Datadog interceptor
     * Enables distributed tracing for specified hosts
     */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // 必须排在 DatadogInterceptor 之前:记录 resource 时 flow_id tag 已就位。
            .addInterceptor(FlowTaggingInterceptor())
            .addInterceptor(
                DatadogInterceptor.Builder(tracedHostsWithHeaderType)
                    .setRumResourceAttributesProvider(FlowResourceAttributesProvider())
                    .build()
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Create a custom OkHttpClient with specific traced hosts
     * @param hosts Map of hostname to tracing header types
     */
    fun createCustomClient(hosts: Map<String, Set<TracingHeaderType>>): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                DatadogInterceptor.Builder(hosts).build()
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
