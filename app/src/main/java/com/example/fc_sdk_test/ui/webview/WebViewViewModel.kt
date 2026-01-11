package com.example.fc_sdk_test.ui.webview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WebViewViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "WebView Fragment"
    }
    val text: LiveData<String> = _text
}
