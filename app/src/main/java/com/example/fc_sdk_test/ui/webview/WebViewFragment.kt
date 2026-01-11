package com.example.fc_sdk_test.ui.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fc_sdk_test.databinding.FragmentWebviewBinding
import android.util.Log
import cloud.flashcat.android.webview.WebViewTracking

class WebViewFragment : Fragment() {

    companion object {
        private const val TAG = "WebViewFragment"
        private const val DEFAULT_URL = "http://192.168.31.7:5173"  // Android emulator localhost
    }

    private var _binding: FragmentWebviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val webViewViewModel = ViewModelProvider(this).get(WebViewViewModel::class.java)

        _binding = FragmentWebviewBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupWebView()
        
        // Enable WebView tracking for RUM
        enableWebViewTracking()
        
        // Load URL
        binding.webView.loadUrl(DEFAULT_URL)
        Log.d(TAG, "Loading URL: $DEFAULT_URL")

        return root
    }

    private fun enableWebViewTracking() {
        try {
            // Enable WebView tracking with allowed hosts
            val allowedHosts = listOf(
                "10.0.2.2",           // Android emulator localhost
                "localhost",          // Direct localhost
                "127.0.0.1",          // Loopback address
                "192.168.31.7"
            )
            WebViewTracking.enable(binding.webView, allowedHosts)
            Log.d(TAG, "✓ WebView RUM tracking enabled")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to enable WebView RUM tracking", e)
        }
    }

    private fun setupWebView() {
        binding.webView.apply {
            // Enable JavaScript
            settings.javaScriptEnabled = true
            
            // Enable DOM storage (needed for modern web apps)
            settings.domStorageEnabled = true
            
            // Enable database
            settings.databaseEnabled = true
            
            // Enable zoom controls
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            
            // Enable local storage
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            
            // For better compatibility
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // Enable cache for better performance
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            
            // Set WebViewClient to handle page navigation
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d(TAG, "Page started loading: $url")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "✓ Page loaded successfully: $url")
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "✗ WebView error: $description (code: $errorCode) for URL: $failingUrl")
                    
                    // Load error page with helpful information
                    val errorHtml = """
                        <html>
                        <body style='font-family: sans-serif; padding: 20px; background: #f5f5f5;'>
                            <h2 style='color: #d32f2f;'>⚠️ Connection Failed</h2>
                            <p><strong>Error:</strong> $description</p>
                            <p><strong>Error Code:</strong> $errorCode</p>
                            <p><strong>URL:</strong> $failingUrl</p>
                            <hr>
                            <h3>Troubleshooting:</h3>
                            <ol>
                                <li>Make sure your dev server is running on port 5173</li>
                                <li>Server should bind to <code>0.0.0.0</code> not <code>127.0.0.1</code></li>
                                <li>Example for Vite: <code>vite --host 0.0.0.0</code></li>
                                <li>Check firewall settings</li>
                            </ol>
                        </body>
                        </html>
                    """.trimIndent()
                    view?.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
                }

                @Deprecated("Deprecated in Java")
                override fun onReceivedHttpError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    errorResponse: android.webkit.WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    Log.e(TAG, "✗ HTTP error: ${errorResponse?.statusCode} for ${request?.url}")
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: android.webkit.SslErrorHandler?,
                    error: android.net.http.SslError?
                ) {
                    Log.e(TAG, "✗ SSL error: ${error?.toString()}")
                    // For development, you might want to proceed anyway
                    // handler?.proceed()
                    super.onReceivedSslError(view, handler, error)
                }
            }
            
            // Set WebChromeClient to handle console messages
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                    message?.let {
                        Log.d(TAG, "WebView Console: ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                    }
                    return true
                }
                
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    Log.d(TAG, "Loading progress: $newProgress%")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
