package com.example.fc_sdk_test.ui.reflow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fc_sdk_test.databinding.FragmentReflowBinding
import com.example.fc_sdk_test.util.ObfuscationTestHelper
import cloud.flashcat.android.rum.GlobalRumMonitor
import cloud.flashcat.android.rum.RumErrorSource
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.net.URL

class ReflowFragment : Fragment() {

    private var _binding: FragmentReflowBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    
    private var isPinging = false
    private var pingThread: Thread? = null
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "ReflowFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val reflowViewModel =
            ViewModelProvider(this).get(ReflowViewModel::class.java)

        _binding = FragmentReflowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textReflow
        reflowViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Set up button click listener to throw exception
        binding.btnThrowException.setOnClickListener {
            throw RuntimeException("Test exception thrown from ReflowFragment button click")
        }

        // Set up ping button click listener
        binding.btnPingBaidu.setOnClickListener {
            if (isPinging) {
                stopPing()
            } else {
                startPing()
            }
        }

        // Set up send logs button click listener
        binding.btnSendLogs.setOnClickListener {
            sendLogsRequest()
        }

        // Set up check proxy button click listener
        binding.btnCheckProxy.setOnClickListener {
            checkProxyStatus()
        }

        // Set up report error button click listener
        binding.btnReportError.setOnClickListener {
            reportErrorToFlashCat()
        }

        // Set up obfuscation test button click listener
        binding.btnTestObfuscation.setOnClickListener {
            testObfuscation()
        }

        return root
    }

    private fun startPing() {
        isPinging = true
        binding.btnPingBaidu.text = "Stop Ping Baidu"
        
        pingThread = Thread {
            while (isPinging) {
                try {
                    val url = URL("https://www.baidu.com")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    
                    val startTime = System.currentTimeMillis()
                    val responseCode = connection.responseCode
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    
                    Log.d(TAG, "Ping baidu.com: response code=$responseCode, duration=${duration}ms")
                    
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Ping baidu.com failed", e)
                }
                
                // Wait 1 second before next ping
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        pingThread?.start()
    }

    private fun stopPing() {
        isPinging = false
        binding.btnPingBaidu.text = "Start Ping Baidu"
        pingThread?.interrupt()
        pingThread = null
    }

    private fun checkProxyStatus() {
        Thread {
            try {
                Log.d(TAG, "========== Proxy Status Check ==========")
                
                // Method 1: Check System Properties
                val httpProxyHost = System.getProperty("http.proxyHost")
                val httpProxyPort = System.getProperty("http.proxyPort")
                val httpsProxyHost = System.getProperty("https.proxyHost")
                val httpsProxyPort = System.getProperty("https.proxyPort")
                
                Log.d(TAG, "System Properties:")
                Log.d(TAG, "  http.proxyHost: ${httpProxyHost ?: "None"}")
                Log.d(TAG, "  http.proxyPort: ${httpProxyPort ?: "None"}")
                Log.d(TAG, "  https.proxyHost: ${httpsProxyHost ?: "None"}")
                Log.d(TAG, "  https.proxyPort: ${httpsProxyPort ?: "None"}")
                
                // Method 2: Use ProxySelector to check system proxy
                val proxySelector = ProxySelector.getDefault()
                val targetUri = URI("https://jira.flashcat.cloud/api/logs")
                val proxies = proxySelector.select(targetUri)
                
                Log.d(TAG, "ProxySelector Results:")
                if (proxies.isEmpty()) {
                    Log.w(TAG, "  ⚠️ No proxy found via ProxySelector!")
                } else {
                    proxies.forEachIndexed { index, proxy ->
                        Log.d(TAG, "  Proxy $index: ${proxy.type()} - ${proxy.address()}")
                        if (proxy.address() is InetSocketAddress) {
                            val addr = proxy.address() as InetSocketAddress
                            Log.d(TAG, "    Host: ${addr.hostName}")
                            Log.d(TAG, "    Port: ${addr.port}")
                        }
                    }
                }
                
                // Method 3: Test actual connection with proxy detection
                val url = URL("https://jira.flashcat.cloud/api/logs")
                
                // Try to detect which proxy will be used
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                // Get proxy info from connection (if available)
                try {
                    val proxyField = connection.javaClass.getDeclaredField("proxy")
                    proxyField.isAccessible = true
                    val proxy = proxyField.get(connection) as? Proxy
                    if (proxy != null && proxy.type() != Proxy.Type.DIRECT) {
                        Log.d(TAG, "Connection will use proxy: ${proxy.address()}")
                    } else {
                        Log.w(TAG, "⚠️ Connection will use DIRECT (no proxy)")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Cannot access proxy field: ${e.message}")
                }
                
                connection.disconnect()
                
                Log.d(TAG, "=========================================")
                
                handler.post {
                    val proxyInfo = if (proxies.isNotEmpty() && proxies[0].type() != Proxy.Type.DIRECT) {
                        val addr = proxies[0].address() as? InetSocketAddress
                        "Proxy: ${addr?.hostName}:${addr?.port}"
                    } else {
                        "⚠️ No proxy detected!"
                    }
                    Toast.makeText(context, proxyInfo, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Proxy check failed", e)
                handler.post {
                    Toast.makeText(context, "Proxy check failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun sendLogsRequest() {
        handler.post {
            binding.btnSendLogs.isEnabled = false
            binding.btnSendLogs.text = "Sending..."
            Toast.makeText(context, "Sending request...", Toast.LENGTH_SHORT).show()
        }
        
        Thread {
            try {
                val urlString = "https://jira.flashcat.cloud/api/logs"
                val url = URL(urlString)
                
                // Log request details before sending
                Log.d(TAG, "========== HTTP Request Details ==========")
                Log.d(TAG, "URL: $urlString")
                Log.d(TAG, "Method: GET")
                
                // Check system proxy settings
                val systemProxy = System.getProperty("http.proxyHost")
                val systemProxyPort = System.getProperty("http.proxyPort")
                val httpsProxy = System.getProperty("https.proxyHost")
                val httpsProxyPort = System.getProperty("https.proxyPort")
                
                Log.d(TAG, "System Proxy Settings:")
                Log.d(TAG, "  HTTP Proxy: ${systemProxy ?: "None"}:${systemProxyPort ?: "None"}")
                Log.d(TAG, "  HTTPS Proxy: ${httpsProxy ?: "None"}:${httpsProxyPort ?: "None"}")
                
                // Check ProxySelector
                val proxySelector = ProxySelector.getDefault()
                val targetUri = URI(urlString)
                val proxies = proxySelector.select(targetUri)
                
                Log.d(TAG, "ProxySelector Detection:")
                var useProxy: Proxy? = null
                if (proxies.isEmpty() || proxies[0].type() == Proxy.Type.DIRECT) {
                    Log.w(TAG, "  ⚠️ WARNING: No proxy detected via ProxySelector!")
                    // Try to get proxy from system properties as fallback
                    val proxyHost = httpsProxy ?: systemProxy
                    val proxyPort = httpsProxyPort ?: systemProxyPort
                    if (proxyHost != null && proxyPort != null) {
                        try {
                            val port = proxyPort.toInt()
                            useProxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, port))
                            Log.d(TAG, "  ✓ Using proxy from system properties: $proxyHost:$port")
                        } catch (e: Exception) {
                            Log.e(TAG, "  Failed to create proxy from system properties", e)
                        }
                    }
                } else {
                    useProxy = proxies[0]
                    proxies.forEach { proxy ->
                        if (proxy.address() is InetSocketAddress) {
                            val addr = proxy.address() as InetSocketAddress
                            Log.d(TAG, "  ✓ Proxy detected: ${addr.hostName}:${addr.port}")
                        }
                    }
                }
                
                // Create connection with explicit proxy if needed
                val connection = if (useProxy != null && useProxy.type() != Proxy.Type.DIRECT) {
                    url.openConnection(useProxy) as HttpURLConnection
                } else {
                    url.openConnection() as HttpURLConnection
                }
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                // Add custom headers to make it easier to identify in Charles
                connection.setRequestProperty("User-Agent", "FCSDKTestApp/1.0")
                connection.setRequestProperty("X-Request-ID", System.currentTimeMillis().toString())
                
                // Log request headers
                Log.d(TAG, "Request Headers:")
                connection.requestProperties.forEach { (key, value) ->
                    Log.d(TAG, "  $key: ${value.joinToString(", ")}")
                }
                
                val startTime = System.currentTimeMillis()
                connection.connect()
                
                // Try to detect actual proxy used (after connection)
                try {
                    val proxyField = connection.javaClass.getDeclaredField("proxy")
                    proxyField.isAccessible = true
                    val actualProxy = proxyField.get(connection) as? Proxy
                    if (actualProxy != null) {
                        if (actualProxy.type() == Proxy.Type.DIRECT) {
                            Log.w(TAG, "⚠️ ACTUAL PROXY USED: DIRECT (no proxy) - Charles won't see this!")
                        } else if (actualProxy.address() is InetSocketAddress) {
                            val addr = actualProxy.address() as InetSocketAddress
                            Log.d(TAG, "✓ ACTUAL PROXY USED: ${addr.hostName}:${addr.port}")
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Cannot detect actual proxy: ${e.message}")
                }
                
                val responseCode = connection.responseCode
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                // Log response headers
                Log.d(TAG, "========== HTTP Response Details ==========")
                Log.d(TAG, "Response Code: $responseCode")
                Log.d(TAG, "Response Message: ${connection.responseMessage}")
                Log.d(TAG, "Duration: ${duration}ms")
                Log.d(TAG, "Response Headers:")
                connection.headerFields.forEach { (key, value) ->
                    Log.d(TAG, "  $key: ${value?.joinToString(", ")}")
                }
                
                // Read response body
                val inputStream = if (responseCode >= 200 && responseCode < 300) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                
                val response = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                
                Log.d(TAG, "Response Body (first 500 chars):")
                Log.d(TAG, if (response.length > 500) response.substring(0, 500) + "..." else response)
                Log.d(TAG, "Response Body Length: ${response.length} bytes")
                Log.d(TAG, "===========================================")
                
                handler.post {
                    val message = "Request completed!\nCode: $responseCode\nDuration: ${duration}ms\nCheck Logcat for details"
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    binding.btnSendLogs.isEnabled = true
                    binding.btnSendLogs.text = "Send Logs Request"
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "========== Request Failed ==========", e)
                Log.e(TAG, "Error Type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error Message: ${e.message}")
                e.printStackTrace()
                Log.e(TAG, "====================================")
                
                handler.post {
                    val errorMsg = "Request failed: ${e.message}\nCheck Logcat for details"
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    binding.btnSendLogs.isEnabled = true
                    binding.btnSendLogs.text = "Send Logs Request"
                }
            }
        }.start()
    }

    private fun reportErrorToFlashCat() {
        try {
            // Create a custom error with stack trace
            val errorMessage = "Test error from ReflowFragment - Report Error button clicked"
            val exception = RuntimeException(errorMessage).apply {
                // Fill in stack trace
                fillInStackTrace()
            }
            
            // Get stack trace as string
            val stackTrace = StringWriter().apply {
                exception.printStackTrace(PrintWriter(this))
            }.toString()
            
            Log.d(TAG, "========== Reporting Error to FlashCat ==========")
            Log.d(TAG, "Error Message: $errorMessage")
            Log.d(TAG, "Stack Trace:\n$stackTrace")
            
            // Report error to FlashCat RUM with stack trace information
            GlobalRumMonitor.get().addError(
                message = errorMessage,
                throwable = exception,
                source = cloud.flashcat.android.rum.RumErrorSource.SOURCE,
                attributes = mapOf(
                    "error_type" to "manual_test_error",
                    "fragment" to "ReflowFragment",
                    "button" to "btn_report_error",
                    "stack_trace" to stackTrace,
                    "timestamp" to System.currentTimeMillis().toString()
                )
            )
            
            Log.d(TAG, "✓ Error reported to FlashCat RUM successfully")
            Log.d(TAG, "================================================")
            
            handler.post {
                Toast.makeText(
                    context,
                    "Error reported to FlashCat RUM\nCheck Logcat for details",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report error to FlashCat RUM", e)
            handler.post {
                Toast.makeText(
                    context,
                    "Failed to report error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Test method to verify obfuscation and deobfuscation.
     * This method calls ObfuscationTestHelper which has 2-3 layer call stack.
     */
    private fun testObfuscation() {
        try {
            Log.d(TAG, "========== Obfuscation Test ==========")
            
            val helper = ObfuscationTestHelper()
            
            // Test 1: Call method with 2-3 layer call stack that throws exception
            Log.d(TAG, "Test 1: Testing deep stack trace (4 levels)")
            val exception = helper.createDeepStackTrace()
            throw exception
            
        } catch (e: Exception) {
            // Log the obfuscated stack trace
            Log.d(TAG, "Obfuscated stack trace:")
            e.printStackTrace()
            
            // Get stack trace as string for logging
            val stackTrace = StringWriter().apply {
                e.printStackTrace(PrintWriter(this))
            }.toString()
            
            Log.d(TAG, "Full stack trace:\n$stackTrace")
            
            // Report to Datadog - this should be deobfuscated on the backend
            GlobalRumMonitor.get().addError(
                message = "Obfuscation test error - multi-layer call stack",
                source = RumErrorSource.SOURCE,
                throwable = e,
                attributes = mapOf(
                    "test_type" to "obfuscation_test",
                    "fragment" to "ReflowFragment",
                    "button" to "btn_test_obfuscation",
                    "stack_trace" to stackTrace,
                    "timestamp" to System.currentTimeMillis().toString()
                )
            )
            
            Log.d(TAG, "✓ Obfuscation test error reported to Datadog")
            Log.d(TAG, "Check Datadog console to see if stack trace is deobfuscated")
            Log.d(TAG, "==========================================")
            
            handler.post {
                Toast.makeText(
                    context,
                    "Obfuscation test completed\nCheck Logcat and Datadog console",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        stopPing()
        super.onDestroyView()
        _binding = null
    }
}