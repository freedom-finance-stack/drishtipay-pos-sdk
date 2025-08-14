@file:OptIn(ExperimentalMaterial3Api::class)

package com.freedomfinancestack.razorpay_drishtipay_test

import android.nfc.NdefMessage
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import com.freedomfinancestack.razorpay_drishtipay_test.ui.theme.RazorpaydrishtipaytestTheme
import com.freedomfinancestack.pos_sdk_core.implementations.PosNfcDeviceManager
import com.freedomfinancestack.pos_sdk_core.interfaces.INfcDeviceManager
import com.freedomfinancestack.razorpay_drishtipay_test.pos.PaxNeptuneLitePlugin
import com.freedomfinancestack.razorpay_drishtipay_test.savedcards.ListSavedCards
import com.freedomfinancestack.razorpay_drishtipay_test.payment.InitiatePayment
import com.freedomfinancestack.pos_sdk_core.models.Card

class MainActivity : ComponentActivity() {
    
    private lateinit var nfcManager: INfcDeviceManager
    private lateinit var paxPlugin: PaxNeptuneLitePlugin
    private lateinit var cardsService: ListSavedCards
    private lateinit var paymentService: InitiatePayment
    private var isListening = mutableStateOf(false)
    private var sdkStatus = mutableStateOf("Not Initialized")
    private var lastPaymentData = mutableStateOf("No payments processed")
    private var pluginMode = mutableStateOf("Mock Mode")
    private var logMessages = mutableStateOf(listOf<String>())
    
    // These will be managed inside Composable
    @Volatile private var savedCardsList: List<Card> = emptyList()
    @Volatile private var showSavedCards: Boolean = false
    @Volatile private var paymentResponse: String = ""
    
    // 🔥 FIX: Make WebView state observable with mutableStateOf
    private var webViewContent = mutableStateOf("")
    private var showWebView = mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            RazorpaydrishtipaytestTheme {
                DrishtiPayDemoScreen()
            }
        }
        
        // Initialize DrishtiPay POS SDK
        initializeDrishtiPaySDK()
    }
    
    @Composable
    fun DrishtiPayDemoScreen() {
        Log.d("VarunDebug", "🔥 DrishtiPayDemoScreen: Starting composition")
        val scrollState = rememberScrollState()
        
        // Force recomposition with simple state
        var forceRefresh by remember { mutableStateOf(0) }
        var savedCardsState by remember { mutableStateOf(emptyList<Card>()) }
        var showSavedCardsState by remember { mutableStateOf(false) }
        var paymentResponseState by remember { mutableStateOf("") }
        
        // 🔥 FIX: Sync local state with global WebView state
        var webViewContentState by remember { mutableStateOf("") }
        var showWebViewState by remember { mutableStateOf(false) }
        
        // 🔥 NEW: Add state synchronization effect with auto-scroll
        LaunchedEffect(showWebView.value, webViewContent.value) {
            Log.d("VarunDebug", "🔥🌐 ========= LaunchedEffect TRIGGERED =========")
            Log.d("VarunDebug", "🔥🌐 Global state: showWebView=${showWebView.value}")
            Log.d("VarunDebug", "🔥🌐 Global state: webViewContent.length=${webViewContent.value.length}")
            Log.d("VarunDebug", "🔥🌐 Global state: webViewContent.isEmpty()=${webViewContent.value.isEmpty()}")
            
            webViewContentState = webViewContent.value
            showWebViewState = showWebView.value
            
            // 🔥 AUTO-SCROLL: When WebView appears, scroll to the bottom to show it
            if (showWebView.value && webViewContent.value.isNotEmpty()) {
                Log.d("VarunDebug", "🔥🌐 Auto-scrolling to WebView...")
                kotlinx.coroutines.delay(500) // Small delay to ensure WebView is rendered
                scrollState.animateScrollTo(scrollState.maxValue)
                Log.d("VarunDebug", "🔥🌐 Auto-scroll completed")
            }
            
            Log.d("VarunDebug", "🔥🌐 Local state updated: showWebViewState=$showWebViewState")
            Log.d("VarunDebug", "🔥🌐 Local state updated: webViewContentState.length=${webViewContentState.length}")
            Log.d("VarunDebug", "🔥🌐 ========= LaunchedEffect COMPLETED =========")
        }
        
        Log.d("VarunDebug", "🔥 DrishtiPayDemoScreen: savedCardsState size = ${savedCardsState.size}")
        Log.d("VarunDebug", "🔥 DrishtiPayDemoScreen: showSavedCardsState = $showSavedCardsState")
        Log.d("VarunDebug", "🔥 DrishtiPayDemoScreen: forceRefresh = $forceRefresh")
        Log.d("VarunDebug", "🔥🌐 DrishtiPayDemoScreen: showWebViewState = $showWebViewState, webViewContent length = ${webViewContentState.length}")
        
        // Direct state management - no LaunchedEffect needed for cards
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("DrishtiPay POS SDK Demo") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 🌐 3DS AUTHENTICATION WEBVIEW - SHOW AT TOP WHEN ACTIVE!
                Log.d("VarunDebug", "🔥🌐 ========= WEBVIEW CONDITION CHECK =========")
                Log.d("VarunDebug", "🔥🌐 showWebViewState = $showWebViewState")
                Log.d("VarunDebug", "🔥🌐 webViewContentState.isNotEmpty() = ${webViewContentState.isNotEmpty()}")
                Log.d("VarunDebug", "🔥🌐 webViewContentState.length = ${webViewContentState.length}")
                Log.d("VarunDebug", "🔥🌐 Condition result = ${showWebViewState && webViewContentState.isNotEmpty()}")
                
                if (showWebViewState && webViewContentState.isNotEmpty()) {
                    Log.d("VarunDebug", "🔥🌐 ========= RENDERING WEBVIEW SECTION AT TOP =========")
                    
                    // 🚨 NOTICE BANNER - Alternative inline option
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Green.copy(alpha = 0.8f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🔐 3DS Authentication Available",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Fullscreen mode opened automatically\n• Inline WebView available below (alternative)",
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // WebView Section - Now more prominent
                    ThreeDSWebViewSection(webViewContentState) {
                        // Close WebView callback - Update both local AND global state
                        Log.d("VarunDebug", "🔥🌐 WebView close button clicked!")
                        showWebViewState = false
                        webViewContentState = ""
                        showWebView.value = false
                        webViewContent.value = ""
                        Log.d("VarunDebug", "🔥🌐 WebView closed, both local and global state reset")
                    }
                } else {
                    Log.d("VarunDebug", "🔥🌐 WEBVIEW NOT RENDERED - condition failed")
                    
                    // 🚨 ONLY SHOW CARDS SECTION WHEN WEBVIEW IS NOT ACTIVE
                    Log.d("VarunDebug", "🔥 COLUMN COMPOSITION: Before SavedCardsSection check: showSavedCardsState = $showSavedCardsState")
                    
                    // Saved Cards Section (only show when cards are loaded and no WebView)
                    if (showSavedCardsState) {
                        Log.d("VarunDebug", "🔥 SHOWING SavedCardsSection with ${savedCardsState.size} cards!")
                        SavedCardsSection(savedCardsState) { card ->
                            Log.d("VarunDebug", "🔥 Card clicked: ${card.last4Digits}")
                            initiatePaymentForCard(card) { response ->
                                Log.d("VarunDebug", "🔥 Payment response received")
                                paymentResponseState = response
                            }
                        }
                    } else {
                        Log.d("VarunDebug", "🔥 NOT showing SavedCardsSection")
                    }
                    
                    // Tarun's Test Section - bilkul simple callback!
                    VarunTestSection { newCards, showCards ->
                        Log.d("VarunDebug", "🔥 Callback received: ${newCards.size} cards, show = $showCards")
                        savedCardsState = newCards
                        showSavedCardsState = showCards
                        forceRefresh++ // Force recomposition
                        Log.d("VarunDebug", "🔥 State updated: savedCardsState = ${savedCardsState.size}, showSavedCardsState = $showSavedCardsState, forceRefresh = $forceRefresh")
                    }
                    
                    // Payment Response Section (only show when payment is made)
                    if (paymentResponseState.isNotEmpty()) {
                        PaymentResponseSection(paymentResponseState) {
                            paymentResponseState = ""
                        }
                    }
                }
                
                // 🚫 COMMENTED OUT - NOT NEEDED FOR TARUN'S PAYMENT TEST
                /*
                // SDK Status Card
                StatusCard()
                
                // Control Buttons
                ControlButtonsSection()
                
                // Mode Configuration
                ModeConfigurationSection()
                
                // Payment Information
                PaymentInfoSection()
                
                // Logs Section
                LogsSection()
                */
            }
        }
    }
    
    @Composable
    fun StatusCard() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isListening.value) 
                    MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "SDK Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Status: ${sdkStatus.value}")
                Text("Mode: ${pluginMode.value}")
                Text("Listening: ${if (isListening.value) "Active" else "Inactive"}")
                if (::paxPlugin.isInitialized) {
                    Text("Config: ${paxPlugin.configInfo}")
                }
            }
        }
    }
    
    @Composable
    fun ControlButtonsSection() {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "NFC Controls",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { startNfcListening() },
                        enabled = !isListening.value,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Listening")
                    }
                    
                    Button(
                        onClick = { stopNfcListening() },
                        enabled = isListening.value,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Stop Listening")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Simulation button (only in mock mode)
                Button(
                    onClick = { simulateNfcTap() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isListening.value && pluginMode.value == "Mock Mode",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Simulate NFC Tap (Emulator Testing)")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { testSavedCards() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("🃏 Test Saved Cards API")
                }
            }
        }
    }
    
    @Composable
    fun ModeConfigurationSection() {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Plugin Configuration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { switchToMockMode() },
                        enabled = !isListening.value,
                        modifier = Modifier.weight(1f),
                        colors = if (pluginMode.value == "Mock Mode") 
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        else ButtonDefaults.buttonColors()
                    ) {
                        Text("Mock Mode")
                    }
                    
                    Button(
                        onClick = { switchToRealMode() },
                        enabled = !isListening.value,
                        modifier = Modifier.weight(1f),
                        colors = if (pluginMode.value == "Real PAX Mode") 
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        else ButtonDefaults.buttonColors()
                    ) {
                        Text("Real PAX Mode")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Mock Mode: For emulator testing without real PAX hardware\nReal PAX Mode: For actual PAX A920/A930 devices",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    @Composable
    fun VarunTestSection(onCardsLoaded: (List<Card>, Boolean) -> Unit) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🚀 Tarun's Payment Test",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { 
                        Log.d("VarunDebug", "🔥 Varun Start button CLICKED!")
                        startVarunTest(onCardsLoaded) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("🎯 Tarun Start", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "This will:\n1. Load saved cards\n2. Show cards for selection\n3. Process payment on card tap",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    @Composable
    fun SavedCardsSection(cards: List<Card>, onCardClick: (Card) -> Unit) {
        Log.d("VarunDebug", "🔥🔥🔥 SavedCardsSection: FUNCTION ENTRY - COMPOSING with ${cards.size} cards")
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Yellow.copy(alpha = 0.3f) // Yellow background to see if it renders
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Big title to make it obvious
                Text(
                    text = "🔥 CARDS SECTION - ${cards.size} CARDS FOUND! 🔥",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Simple list of cards - no fancy UI
                cards.forEachIndexed { index, card ->
                    Log.d("VarunDebug", "🔥 Creating UI for card $index: ****${card.last4Digits}")
                    
                    // Simple card display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Simple text - just bank name and last 4 digits
                        Text(
                            text = "${card.issuerBank} - ****${card.last4Digits}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                        
                        // Simple button
                        Button(
                            onClick = { 
                                Log.d("VarunDebug", "🔥 Card clicked: ****${card.last4Digits}")
                                onCardClick(card)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Green
                            )
                        ) {
                            Text("PAY ₹10", color = Color.White)
                        }
                    }
                    
                    // Divider between cards
                    if (index < cards.size - 1) {
                        HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Hide button
                Button(
                    onClick = { 
                        Log.d("VarunDebug", "🔥 Hide Cards clicked")
                        showSavedCards = false
                        savedCardsList = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("HIDE CARDS", color = Color.White)
                }
            }
        }
    }
    
    @Composable
    fun ThreeDSWebViewSection(htmlContent: String, onClose: () -> Unit) {
        Log.d("VarunDebug", "🔥🌐 ========= ThreeDSWebViewSection COMPOSING =========")
        Log.d("VarunDebug", "🔥🌐 htmlContent.length = ${htmlContent.length}")
        Log.d("VarunDebug", "🔥🌐 htmlContent.isEmpty() = ${htmlContent.isEmpty()}")
        Log.d("VarunDebug", "🔥🌐 htmlContent preview: ${htmlContent.take(100)}...")
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Blue.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔐 Inline 3DS Authentication",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                    
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("✕ Close", color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Complete the authentication below:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // WebView for 3DS authentication - MUCH LARGER!
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp), // Even bigger height - takes most of screen
                    factory = { context ->
                        Log.d("VarunDebug", "🔥🌐 ========= WEBVIEW FACTORY CREATING =========")
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowContentAccess = true
                            settings.allowFileAccess = true
                            settings.setNeedInitialFocus(false)
                            
                            // Enhanced WebView client with better logging
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    Log.d("VarunDebug", "🔥🌐 WebView page started: $url")
                                }
                                
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("VarunDebug", "🔥🌐 WebView page finished loading: $url")
                                }
                                
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    Log.d("VarunDebug", "🔥🌐 WebView URL loading: $url")
                                    return false
                                }
                                
                                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                    super.onReceivedError(view, errorCode, description, failingUrl)
                                    Log.e("VarunDebug", "🔥🌐 WebView error: $errorCode - $description for $failingUrl")
                                }
                            }
                        }
                    },
                    update = { webView ->
                        Log.d("VarunDebug", "🔥🌐 ========= WEBVIEW UPDATE CALLED =========")
                        Log.d("VarunDebug", "🔥🌐 Loading HTML content into WebView (length: ${htmlContent.length})")
                        Log.d("VarunDebug", "🔥🌐 HTML Preview: ${htmlContent.take(200)}...")
                        
                        // Try loadDataWithBaseURL for better compatibility
                        try {
                            webView.loadDataWithBaseURL(
                                null, 
                                htmlContent, 
                                "text/html", 
                                "UTF-8", 
                                null
                            )
                            Log.d("VarunDebug", "🔥🌐 WebView.loadDataWithBaseURL() called successfully")
                        } catch (e: Exception) {
                            Log.e("VarunDebug", "🔥🌐 ERROR loading HTML into WebView: ${e.message}", e)
                        }
                    }
                )
            }
        }
    }
    
    @Composable
    fun PaymentResponseSection(response: String, onClear: () -> Unit) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💰 Payment Response",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = response,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onClear,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text("Clear Response")
                }
            }
        }
    }

    @Composable
    fun PaymentInfoSection() {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Last Payment Data",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lastPaymentData.value,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    
    @Composable
    fun LogsSection() {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity Logs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { clearLogs() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text("Clear")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column {
                    logMessages.value.takeLast(10).forEach { message ->
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    
                    if (logMessages.value.isEmpty()) {
                        Text(
                            text = "No logs yet...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    private fun initializeDrishtiPaySDK() {
        try {
            addLog("Initializing DrishtiPay POS SDK...")
            
            // Create PAX plugin in mock mode by default
            paxPlugin = PaxNeptuneLitePlugin().apply {
                setMockMode(true)
                setAutoSimulation(true, 3000) // Auto-simulate after 3 seconds
            }
            
            // Initialize the POS NFC Device Manager with PAX plugin
            nfcManager = PosNfcDeviceManager(this, paxPlugin)
            
            // Initialize cards service
            cardsService = ListSavedCards()
            
            // Initialize payment service
            paymentService = InitiatePayment()
            
            sdkStatus.value = "Initialized Successfully"
            pluginMode.value = "Mock Mode"
            addLog("✅ DrishtiPay POS SDK initialized successfully!")
            addLog("📱 Ready for emulator testing with mock NFC simulation")
            
        } catch (e: Exception) {
            sdkStatus.value = "Initialization Failed"
            addLog("❌ Failed to initialize DrishtiPay SDK: ${e.message}")
            Log.e("MainActivity", "Failed to initialize DrishtiPay SDK", e)
        }
    }
    
    private fun startNfcListening() {
        try {
            addLog("🎧 Starting NFC listening...")
            
            nfcManager.startListening(object : INfcDeviceManager.NdefCallback {
                override fun onNdefMessageDiscovered(message: NdefMessage) {
                    runOnUiThread {
                        addLog("💳 NFC payment detected!")
                        processPayment(message)
                    }
                }
                
                override fun onError(errorMessage: String) {
                    runOnUiThread {
                        addLog("❌ NFC Error: $errorMessage")
                        Toast.makeText(this@MainActivity, "NFC Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            })
            
            isListening.value = true
            addLog("✅ NFC listening started - ready for customer taps")
            
            if (pluginMode.value == "Mock Mode") {
                addLog("🤖 Mock mode: Will auto-simulate NFC tap in 3 seconds...")
            }
            
        } catch (e: Exception) {
            addLog("❌ Failed to start NFC listening: ${e.message}")
            Toast.makeText(this, "Failed to start NFC: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun stopNfcListening() {
        try {
            addLog("🛑 Stopping NFC listening...")
            nfcManager.stopListening()
            isListening.value = false
            addLog("✅ NFC listening stopped")
        } catch (e: Exception) {
            addLog("❌ Error stopping NFC: ${e.message}")
        }
    }
    
    private fun simulateNfcTap() {
        if (::paxPlugin.isInitialized && pluginMode.value == "Mock Mode") {
            addLog("🎯 Manually triggering NFC simulation...")
            paxPlugin.triggerTestNfcTap()
        }
    }
    
    private fun testSavedCards() {
        if (!::cardsService.isInitialized) {
            addLog("❌ Cards service not initialized")
            return
        }
        
        try {
            addLog("🃏 Testing Saved Cards API...")
            
            // Call the saved cards API with mock data
            val merchantId = "CO9vBE2ZlgawZYVDx2Y9"
            val contact = "+918955496900"
            
            val savedCardsList = cardsService.listAllSavedCards(merchantId, contact)
            
            addLog("✅ Saved Cards API called successfully!")
            addLog("📋 Found ${savedCardsList.size} saved card collections")
            
            savedCardsList.forEach { savedCards ->
                addLog("📱 Contact: ${savedCards.contact}")
                addLog("💳 Number of cards: ${savedCards.cards.size}")
                
                savedCards.cards.forEach { card ->
                    addLog("   • Card: ****${card.last4Digits} (${card.network} ${card.cardType} - ${card.issuerBank})")
                }
            }
            
            // Display in payment data section
            val cardsInfo = savedCardsList.flatMap { it.cards.toList() }
                .joinToString("\n") { "💳 ****${it.last4Digits} - ${it.network} ${it.cardType} (${it.issuerBank})" }
            
            lastPaymentData.value = "SAVED CARDS TEST:\n\n$cardsInfo\n\n✅ Mock cards loaded successfully!"
            
            Toast.makeText(this, "Saved Cards loaded: ${cardsInfo.split("\n").size-3} cards", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            addLog("❌ Error testing saved cards: ${e.message}")
            Toast.makeText(this, "Error loading saved cards: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun switchToMockMode() {
        try {
            addLog("🔄 Switching to Mock Mode...")
            paxPlugin.setMockMode(true)
            pluginMode.value = "Mock Mode"
            addLog("✅ Switched to Mock Mode - suitable for emulator testing")
        } catch (e: Exception) {
            addLog("❌ Failed to switch to mock mode: ${e.message}")
        }
    }
    
    private fun switchToRealMode() {
        try {
            addLog("🔄 Switching to Real PAX Mode...")
            paxPlugin.setMockMode(false)
            pluginMode.value = "Real PAX Mode"
            addLog("⚠️ Switched to Real PAX Mode - requires actual PAX hardware")
            addLog("📝 Note: Real PAX SDK integration is placeholder - will use mock fallback")
        } catch (e: Exception) {
            addLog("❌ Failed to switch to real mode: ${e.message}")
        }
    }
    
    private fun processPayment(message: NdefMessage) {
        try {
            // Extract payment data from NDEF message
            val paymentData = extractPaymentData(message)
            
            addLog("💰 Processing payment: $paymentData")
            lastPaymentData.value = paymentData
            
            // Simulate payment processing
            addLog("🔄 Sending to payment gateway...")
            
            // In real implementation, you would:
            // 1. Parse the NDEF message for payment data
            // 2. Validate the payment request
            // 3. Send to your payment processor (Razorpay, etc.)
            // 4. Handle the response
            
            // For demo, just simulate success
            Thread {
                Thread.sleep(2000) // Simulate processing time
                runOnUiThread {
                    addLog("✅ Payment successful! Transaction processed.")
                    Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show()
                    
                    // Stop listening after successful payment
                    stopNfcListening()
                }
            }.start()
            
        } catch (e: Exception) {
            addLog("❌ Payment processing failed: ${e.message}")
            Toast.makeText(this, "Payment processing failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun extractPaymentData(message: NdefMessage): String {
        return try {
            if (message.records.isNotEmpty()) {
                val payload = message.records[0].payload
                String(payload)
            } else {
                "No payment data found"
            }
        } catch (e: Exception) {
            "Error extracting payment data: ${e.message}"
        }
    }
    
    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        
        logMessages.value = logMessages.value + logEntry
        Log.d("MainActivity", message)
    }
    
    private fun clearLogs() {
        logMessages.value = emptyList()
        addLog("📋 Logs cleared")
    }
    
    private fun startVarunTest(onCardsLoaded: (List<Card>, Boolean) -> Unit) {
        Log.d("VarunDebug", "🔥 startVarunTest: FUNCTION CALLED!")
        addLog("🚀 Varun Start button clicked!")
        
        try {
            Log.d("VarunDebug", "🔥 startVarunTest: Starting card loading process")
            addLog("📋 Loading saved cards...")
            
            if (!::cardsService.isInitialized) {
                Log.e("VarunDebug", "🔥 ERROR: cardsService not initialized!")
                addLog("❌ cardsService not initialized!")
                return
            }
            
            Log.d("VarunDebug", "🔥 cardsService is initialized, proceeding...")
            
            val merchantId = "CO9vBE2ZlgawZYVDx2Y9"
            val contact = "+918955496900"
            
            Log.d("VarunDebug", "🔥 Calling listAllSavedCards with merchantId: $merchantId, contact: $contact")
            val savedCardsResponse = cardsService.listAllSavedCards(merchantId, contact)
            Log.d("VarunDebug", "🔥 Got savedCardsResponse with ${savedCardsResponse.size} items")
            
            val cards = savedCardsResponse.flatMap { it.cards.toList() }
            Log.d("VarunDebug", "🔥 Extracted ${cards.size} cards from response")
            
            cards.forEachIndexed { index, card ->
                Log.d("VarunDebug", "🔥 Card $index: ID=${card.cardId}, Last4=${card.last4Digits}, Network=${card.network}")
            }
            
            Log.d("VarunDebug", "🔥 About to call onCardsLoaded callback with ${cards.size} cards")
            
            // Force UI update on main thread
            runOnUiThread {
                onCardsLoaded(cards, true)
                Log.d("VarunDebug", "🔥 onCardsLoaded callback completed on UI thread!")
            }
            
            addLog("✅ Loaded ${cards.size} saved cards")
            addLog("💳 Displaying cards for selection...")
            
            Toast.makeText(this, "Loaded ${cards.size} cards successfully!", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e("VarunDebug", "🔥 ERROR in startVarunTest: ${e.message}", e)
            addLog("❌ Error in Varun test: ${e.message}")
            Toast.makeText(this, "Error loading cards: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun initiatePaymentForCard(card: Card, onResponse: (String) -> Unit) {
        Log.d("VarunTest", "💰 Initiating payment for card ****${card.last4Digits}")
        addLog("💰 Initiating payment for card ****${card.last4Digits}")
        
        if (!::paymentService.isInitialized) {
            Log.e("VarunTest", "❌ paymentService not initialized!")
            addLog("❌ paymentService not initialized!")
            return
        }
        
        // Run payment on background thread
        Thread {
            try {
                Log.d("VarunTest", "🔄 Processing payment...")
                addLog("🔄 Processing payment...")
                
                val response = paymentService.initiatePayment(card, 10.0f)
                Log.d("VarunTest", "Payment response received: $response")
                
                runOnUiThread {
                    if (response != null) {
                        val acsUrl = response.acsURL ?: ""
                        val paymentId = response.paymentId ?: ""
                        val orderId = response.orderId ?: ""
                        
                        // Check if acsURL contains HTML (3DS authentication required)
                        if (acsUrl.contains("<html") || acsUrl.contains("<!DOCTYPE") || acsUrl.contains("<form") || acsUrl.length > 500) {
                            Log.d("VarunTest", "🔥🌐 HTML response detected in acsURL - 3DS authentication required!")
                            Log.d("VarunTest", "🔥🌐 HTML Content Preview: ${acsUrl.take(200)}...")
                            addLog("🔐 3DS Authentication required - opening WebView")
                            
                            // 🔥 OPTION 1: Launch fullscreen WebView activity (RECOMMENDED)
                            Log.d("VarunTest", "🔥🌐 Launching fullscreen 3DS WebView activity")
                            ThreeDSWebViewActivity.start(this@MainActivity, acsUrl, paymentId)
                            
                            // Show response in payment section
                            onResponse("🔐 3DS Authentication opened in fullscreen mode\n\nHTML detected (${acsUrl.length} chars)\n\nCheck the new screen for authentication")
                            
                            Toast.makeText(this@MainActivity, "3DS Authentication opened in fullscreen!", Toast.LENGTH_LONG).show()
                            
                            // 🔥 OPTION 2: Also set inline WebView as backup (user can choose)
                            Log.d("VarunTest", "🔥🌐 ========= SETTING GLOBAL WEBVIEW VARIABLES =========")
                            Log.d("VarunTest", "🔥🌐 Before: webViewContent.length=${webViewContent.value.length}, showWebView=${showWebView.value}")
                            
                            webViewContent.value = acsUrl
                            showWebView.value = true
                            
                            Log.d("VarunTest", "🔥🌐 After: webViewContent.length=${webViewContent.value.length}, showWebView=${showWebView.value}")
                            Log.d("VarunTest", "🔥🌐 Global vars set: webViewContent.isEmpty()=${webViewContent.value.isEmpty()}")
                            Log.d("VarunTest", "🔥🌐 ========= GLOBAL VARIABLES SET COMPLETE =========")
                        } else {
                            // Regular JSON response
                            val responseText = """
                                ✅ Payment Initiated Successfully!
                                
                                Card: ****${card.last4Digits}
                                Network: ${card.network}
                                Type: ${card.cardType}
                                Bank: ${card.issuerBank}
                                Amount: ₹10.00
                                
                                Payment ID: $paymentId
                                Order ID: $orderId
                                ACS URL: $acsUrl
                                
                                Check Logcat for detailed API responses:
                                - Filter by 'RazorpayOrder' for order creation
                                - Filter by 'RazorpayPayment' for payment creation
                            """.trimIndent()
                            
                            onResponse(responseText)
                            addLog("✅ Payment processing completed!")
                            addLog("📱 Check payment response section above")
                            
                            Toast.makeText(this@MainActivity, "Payment processed! Check response above", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        onResponse("❌ Payment failed - no response received")
                        addLog("❌ Payment failed - null response")
                        Log.e("VarunTest", "❌ Payment failed - null response")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("VarunTest", "❌ Payment error: ${e.message}", e)
                runOnUiThread {
                    onResponse("❌ Payment Error: ${e.message}")
                    addLog("❌ Payment error: ${e.message}")
                    Toast.makeText(this@MainActivity, "Payment error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up SDK resources
        try {
            if (::nfcManager.isInitialized) {
                nfcManager.stopListening()
                if (nfcManager is PosNfcDeviceManager) {
                    (nfcManager as PosNfcDeviceManager).cleanup()
                }
            }
            addLog("🧹 SDK resources cleaned up")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during cleanup", e)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RazorpaydrishtipaytestTheme {
        Text("DrishtiPay SDK Demo")
    }
}