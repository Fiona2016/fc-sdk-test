package com.example.fc_sdk_test.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Example API service demonstrating network requests with Datadog tracing
 */
class ApiService {
    
    companion object {
        private const val TAG = "ApiService"
    }
    
    private val client = NetworkManager.okHttpClient
    
    /**
     * Make a GET request to the specified URL
     * @param url Target URL
     * @return Response body as string or error message
     */
    suspend fun get(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Making GET request to: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response: Response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.d(TAG, "Request successful. Response length: ${body.length}")
                Result.success(body)
            } else {
                val errorMsg = "Request failed: ${response.code} ${response.message}"
                Log.e(TAG, errorMsg)
                Result.failure(IOException(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request error", e)
            Result.failure(e)
        }
    }
    
    /**
     * GET with a bounded total call timeout. Reuses the shared client's interceptors (so Datadog
     * tracking + flow_id tagging still apply) via newBuilder(); only caps the per-call duration so a
     * slow/unreachable host fails fast instead of waiting the default 30s.
     */
    suspend fun getWithTimeout(url: String, callTimeoutMs: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val timedClient = client.newBuilder()
                .callTimeout(callTimeoutMs, TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder().url(url).get().build()
            val response = timedClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(response.body?.string() ?: "")
            } else {
                Result.failure(IOException("Request failed: ${response.code} ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request error", e)
            Result.failure(e)
        }
    }

    /**
     * Test endpoint to verify tracing functionality
     */
    suspend fun testHttpBin(): Result<String> {
        return get("https://httpbin.org/get")
    }
    
    /**
     * Test endpoint with delay to observe long-running requests
     */
    suspend fun testHttpBinDelay(delaySeconds: Int = 2): Result<String> {
        return get("https://httpbin.org/delay/$delaySeconds")
    }
    
    /**
     * Test GitHub API endpoint
     */
    suspend fun testGitHubApi(): Result<String> {
        return get("https://api.github.com/users/github")
    }
    
    /**
     * Test JSONPlaceholder endpoint
     */
    suspend fun testJsonPlaceholder(): Result<String> {
        return get("https://jsonplaceholder.typicode.com/posts/1")
    }
    
    /**
     * Test SSL certificate failure scenarios using badssl.com test endpoints.
     * These endpoints are designed to trigger specific SSL/TLS errors.
     */
    suspend fun testCertificateFailure(type: CertFailureType): Result<String> {
        return get(type.url)
    }
    
    /**
     * Types of SSL certificate failures that can be tested
     */
    enum class CertFailureType(val url: String, val description: String) {
        EXPIRED("https://expired.badssl.com/", "Expired Certificate"),
        WRONG_HOST("https://wrong.host.badssl.com/", "Wrong Host"),
        SELF_SIGNED("https://self-signed.badssl.com/", "Self-Signed Certificate"),
        UNTRUSTED_ROOT("https://untrusted-root.badssl.com/", "Untrusted Root CA")
    }
}
