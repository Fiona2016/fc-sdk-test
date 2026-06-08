package com.example.fc_sdk_test.ui.slideshow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fc_sdk_test.databinding.FragmentSlideshowBinding
import com.example.fc_sdk_test.network.ApiService
import com.example.fc_sdk_test.network.ApiService.CertFailureType
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SlideshowFragment : Fragment() {

    companion object {
        private const val TAG = "NetworkTraceFragment"
    }

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!
    private val apiService = ApiService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        
        setupButtons()
        
        return binding.root
    }
    
    private fun setupButtons() {
        // httpbin.org test button
        binding.btnTestHttpbin.setOnClickListener {
            testHttpBin()
        }
        
        // GitHub API test button
        binding.btnTestGithub.setOnClickListener {
            testGitHubApi()
        }
        
        // JSONPlaceholder test button
        binding.btnTestJsonplaceholder.setOnClickListener {
            testJsonPlaceholder()
        }
        
        // SSL Certificate failure test button
        binding.btnTestCertFailure.setOnClickListener {
            testCertificateFailure()
        }
    }
    
    private fun testHttpBin() {
        Log.d(TAG, "Testing httpbin.org...")
        updateResult("Requesting httpbin.org...")
        setButtonsEnabled(false)
        
        viewLifecycleOwner.lifecycleScope.launch {
            val result = apiService.testHttpBin()
            
            result.onSuccess { response ->
                Log.d(TAG, "✓ httpbin.org request successful")
                updateResult("✓ httpbin.org SUCCESS\n\nResponse:\n${response.take(500)}...")
                showSnackbar("httpbin.org request successful!")
            }.onFailure { error ->
                Log.e(TAG, "✗ httpbin.org request failed", error)
                updateResult("✗ httpbin.org FAILED\n\nError: ${error.message}")
                showSnackbar("httpbin.org request failed: ${error.message}")
            }
            
            setButtonsEnabled(true)
        }
    }
    
    private fun testGitHubApi() {
        Log.d(TAG, "Testing GitHub API...")
        updateResult("Requesting api.github.com...")
        setButtonsEnabled(false)
        
        viewLifecycleOwner.lifecycleScope.launch {
            val result = apiService.testGitHubApi()
            
            result.onSuccess { response ->
                Log.d(TAG, "✓ GitHub API request successful")
                updateResult("✓ GitHub API SUCCESS\n\nResponse:\n${response.take(500)}...")
                showSnackbar("GitHub API request successful!")
            }.onFailure { error ->
                Log.e(TAG, "✗ GitHub API request failed", error)
                updateResult("✗ GitHub API FAILED\n\nError: ${error.message}")
                showSnackbar("GitHub API request failed: ${error.message}")
            }
            
            setButtonsEnabled(true)
        }
    }
    
    private fun testJsonPlaceholder() {
        Log.d(TAG, "Testing JSONPlaceholder...")
        updateResult("Requesting jsonplaceholder.typicode.com...")
        setButtonsEnabled(false)
        
        viewLifecycleOwner.lifecycleScope.launch {
            val result = apiService.testJsonPlaceholder()
            
            result.onSuccess { response ->
                Log.d(TAG, "✓ JSONPlaceholder request successful")
                updateResult("✓ JSONPlaceholder SUCCESS\n\nResponse:\n$response")
                showSnackbar("JSONPlaceholder request successful!")
            }.onFailure { error ->
                Log.e(TAG, "✗ JSONPlaceholder request failed", error)
                updateResult("✗ JSONPlaceholder FAILED\n\nError: ${error.message}")
                showSnackbar("JSONPlaceholder request failed: ${error.message}")
            }
            
            setButtonsEnabled(true)
        }
    }
    
    private fun testCertificateFailure() {
        val failureType = CertFailureType.EXPIRED
        Log.d(TAG, "Testing SSL Certificate Failure (${failureType.description})...")
        updateResult("Testing ${failureType.description}...\nURL: ${failureType.url}\n\nExpecting SSL handshake failure...")
        setButtonsEnabled(false)
        
        viewLifecycleOwner.lifecycleScope.launch {
            val result = apiService.testCertificateFailure(failureType)
            
            result.onSuccess { response ->
                // This should NOT happen with default SSL configuration
                Log.w(TAG, "⚠ Certificate test unexpectedly succeeded")
                updateResult("⚠ UNEXPECTED SUCCESS\n\nThe request should have failed due to ${failureType.description}.\nThis may indicate SSL verification is disabled.\n\nResponse:\n${response.take(200)}...")
                showSnackbar("Warning: SSL verification may be disabled!")
            }.onFailure { error ->
                Log.d(TAG, "✓ Certificate failure test completed as expected", error)
                val errorType = error::class.simpleName ?: "Unknown"
                updateResult(
                    "✓ SSL FAILURE TEST PASSED\n\n" +
                    "Failure Type: ${failureType.description}\n" +
                    "URL: ${failureType.url}\n\n" +
                    "Exception: $errorType\n" +
                    "Message: ${error.message}\n\n" +
                    "This confirms SSL certificate validation is working correctly."
                )
                showSnackbar("SSL certificate validation working!")
            }
            
            setButtonsEnabled(true)
        }
    }
    
    private fun updateResult(text: String) {
        binding.textResult.text = text
    }
    
    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnTestHttpbin.isEnabled = enabled
        binding.btnTestGithub.isEnabled = enabled
        binding.btnTestJsonplaceholder.isEnabled = enabled
        binding.btnTestCertFailure.isEnabled = enabled
    }
    
    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
