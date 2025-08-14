package com.freedomfinancestack.razorpay_drishtipay_test

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.freedomfinancestack.razorpay_drishtipay_test.ui.theme.RazorpaydrishtipaytestTheme

@OptIn(ExperimentalMaterial3Api::class)
class ThreeDSWebViewActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_HTML_CONTENT = "html_content"
        const val EXTRA_PAYMENT_ID = "payment_id"
        
        fun start(activity: Activity, htmlContent: String, paymentId: String = "") {
            val intent = Intent(activity, ThreeDSWebViewActivity::class.java)
            intent.putExtra(EXTRA_HTML_CONTENT, htmlContent)
            intent.putExtra(EXTRA_PAYMENT_ID, paymentId)
            activity.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val htmlContent = intent.getStringExtra(EXTRA_HTML_CONTENT) ?: ""
        val paymentId = intent.getStringExtra(EXTRA_PAYMENT_ID) ?: ""
        
        Log.d("ThreeDSWebView", "🔥🌐 3DS WebView Activity created with HTML length: ${htmlContent.length}")
        
        setContent {
            RazorpaydrishtipaytestTheme {
                ThreeDSFullscreenWebView(
                    htmlContent = htmlContent,
                    paymentId = paymentId,
                    onClose = { finish() }
                )
            }
        }
    }
    
    @Composable
    fun ThreeDSFullscreenWebView(
        htmlContent: String,
        paymentId: String,
        onClose: () -> Unit
    ) {
        var isLoading by remember { mutableStateOf(true) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "🔐 3DS Authentication",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Blue,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Fullscreen WebView
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        Log.d("ThreeDSWebView", "🔥🌐 Creating fullscreen WebView")
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowContentAccess = true
                            settings.allowFileAccess = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.setSupportZoom(true)
                            
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    Log.d("ThreeDSWebView", "🔥🌐 Page started: $url")
                                    isLoading = true
                                }
                                
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("ThreeDSWebView", "🔥🌐 Page finished: $url")
                                    isLoading = false
                                    
                                    // Check if this is a success/completion URL
                                    url?.let { currentUrl ->
                                        if (currentUrl.contains("redirect_callback") || 
                                            currentUrl.contains("success") ||
                                            currentUrl.contains("completed")) {
                                            Log.d("ThreeDSWebView", "🔥🌐 Authentication completed! URL: $currentUrl")
                                            // Optionally auto-close after completion
                                            // postDelayed({ onClose() }, 2000)
                                        }
                                    }
                                }
                                
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    Log.d("ThreeDSWebView", "🔥🌐 URL loading: $url")
                                    return false
                                }
                                
                                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                    super.onReceivedError(view, errorCode, description, failingUrl)
                                    Log.e("ThreeDSWebView", "🔥🌐 WebView error: $errorCode - $description for $failingUrl")
                                    isLoading = false
                                }
                            }
                        }
                    },
                    update = { webView ->
                        Log.d("ThreeDSWebView", "🔥🌐 Loading HTML content (${htmlContent.length} chars)")
                        try {
                            webView.loadDataWithBaseURL(
                                null,
                                htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        } catch (e: Exception) {
                            Log.e("ThreeDSWebView", "🔥🌐 Error loading HTML: ${e.message}", e)
                        }
                    }
                )
                
                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.9f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = Color.Blue
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading 3DS Authentication...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
