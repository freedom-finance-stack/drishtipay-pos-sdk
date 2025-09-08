@file:OptIn(ExperimentalMaterial3Api::class)

package com.freedomfinancestack.razorpay_drishtipay_test

import android.nfc.NdefMessage
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedomfinancestack.pos_sdk_core.implementations.PosNfcDeviceManager
import com.freedomfinancestack.pos_sdk_core.interfaces.INfcDeviceManager
import com.freedomfinancestack.pos_sdk_core.models.Card
import com.freedomfinancestack.pos_sdk_core.implementations.NarratorImpl
import com.freedomfinancestack.pos_sdk_core.implementations.GGWaveImpl
import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWave
import com.freedomfinancestack.pos_sdk_core.models.GGWaveMessage
import com.freedomfinancestack.razorpay_drishtipay_test.payment.InitiatePayment
import com.freedomfinancestack.razorpay_drishtipay_test.pos.PaxNeptuneLitePlugin
import com.freedomfinancestack.razorpay_drishtipay_test.savedcards.ListSavedCards
import com.freedomfinancestack.razorpay_drishtipay_test.ui.theme.RazorpaydrishtipaytestTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val AUDIO_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var nfcManager: INfcDeviceManager
    private lateinit var paxPlugin: PaxNeptuneLitePlugin
    private lateinit var cardsService: ListSavedCards
    private lateinit var paymentService: InitiatePayment
    private lateinit var narrator: NarratorImpl
    private lateinit var ggWave: IGGWave
    private var isListening = mutableStateOf(false)
    private var sdkStatus = mutableStateOf("Not Initialized")
    private var lastPaymentData = mutableStateOf("No payments processed")
    private var pluginMode = mutableStateOf("Mock Mode")
    private var logMessages = mutableStateOf(listOf<String>())

    // GGWave specific states
    private var AudioStatus = mutableStateOf("Not Initialized")
    private var ggWaveListening = mutableStateOf(false)
    private var ggWaveLastMessage = mutableStateOf("")
    private var ggWaveReceivedMessages = mutableStateOf(listOf<String>())
    private var ggWaveLogMessages = mutableStateOf(listOf<String>())

    // Cards state for GGWave triggered updates
    private var CardsState = mutableStateOf(listOf<Card>())
    private var ShowCardsState = mutableStateOf(false)
    private var ForceRefresh = mutableStateOf(0)

    // These will be managed inside Composable
    @Volatile
    private var savedCardsList: List<Card> = emptyList()

    @Volatile
    private var showSavedCards: Boolean = false

    @Volatile
    private var paymentResponse: String = ""

    // 🚫 REMOVED: No WebView state needed - using fullscreen activity only!

    // Store 3DS data for potential reopen
    private var lastThreeDSContent: String = ""
    private var lastPaymentId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RazorpaydrishtipaytestTheme {
                DrishtiPayDemoScreen()
            }
        }
        // Initialize Narrator for voice feedback
        narrator = NarratorImpl(this)

        // Check and request audio permission before initializing SDK
        checkAndRequestAudioPermission()
    }

    private fun checkAndRequestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, initialize SDK
            initializeDrishtiPaySDK()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            AUDIO_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, initialize SDK
                    addLog("🎤 Audio permission granted")
                    initializeDrishtiPaySDK()
                } else {
                    // Permission denied, initialize SDK without GGWave functionality
                    addLog("⚠️ Audio permission denied - GGWave will not work")
                    Toast.makeText(this, "Audio permission required for Audio transfer functionality", Toast.LENGTH_LONG).show()
                    initializeDrishtiPaySDKWithoutGGWave()
                }
            }
        }
    }

    @Composable
    fun DrishtiPayDemoScreen() {
        val scrollState = rememberScrollState()

        // Force recomposition with simple state
        var forceRefresh by remember { mutableStateOf(0) }
        var savedCardsState by remember { mutableStateOf(emptyList<Card>()) }
        var showSavedCardsState by remember { mutableStateOf(false) }
        var paymentResponseState by remember { mutableStateOf("") }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // POS Terminal Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color(0xFF1E3A8A),
                                    androidx.compose.ui.graphics.Color(0xFF1E40AF)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DrishtiPay POS",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            Text(
                                text = "Terminal ID: DPT-001",
                                fontSize = 12.sp,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // Status Indicator
                        Box(
                            modifier = Modifier
                                .background(
                                    androidx.compose.ui.graphics.Color(0xFF10B981),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "READY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // NFC Payment Simulation Section
                TestSection { newCards, showCards ->
                    Log.d(
                        "Debug",
                        "Callback received: ${newCards.size} cards, show = $showCards"
                    )
                    savedCardsState = newCards
                    showSavedCardsState = showCards
                    forceRefresh++ // Force recomposition
                    Log.d(
                        "Debug",
                        "State updated: savedCardsState = ${savedCardsState.size}, showSavedCardsState = $showSavedCardsState"
                    )
                }

                // GGWave Audio Communication Section - HIDDEN for magic effect
                // GGWaveDemoSection() // Hidden - works in background for magical card appearance

                // Saved Cards Section (only show when cards are loaded)
                if (showSavedCardsState) {
                    Log.d(
                        "Debug",
                        "SHOWING SavedCardsSection with ${savedCardsState.size} cards!"
                    )
                    SavedCardsSection(savedCardsState) { card ->
                        Log.d("Debug", "Card clicked: ${card.last4Digits}")
                        initiatePaymentForCard(card) { response ->
                            Log.d("Debug", "Payment response received")
                            paymentResponseState = response
                        }
                    }
                }

                // GGWave Triggered Cards Section (only show when triggered by GGWave)
                if (ShowCardsState.value) {
                    Log.d(
                        "Debug",
                        "SHOWING Audio Triggered Cards with ${CardsState.value.size} cards!"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🎵 Audio Triggered Cards",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Cards loaded automatically after receiving ultrasound message",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    SavedCardsSection(CardsState.value) { card ->
                        Log.d("Debug", "Audio triggered card clicked: ${card.last4Digits}")
                        initiatePaymentForCard(card) { response ->
                            Log.d("Debug", "Audio payment response received")
                            paymentResponseState = response
                        }
                    }
                }

                // Payment Response Section (only show when payment is made)
                if (paymentResponseState.isNotEmpty()) {
                    PaymentResponseSection(paymentResponseState) {
                        paymentResponseState = ""
                        // Clear stored 3DS data when response is cleared
                        lastThreeDSContent = ""
                        lastPaymentId = ""
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
    fun TestSection(onCardsLoaded: (List<Card>, Boolean) -> Unit) {
        // Main Payment Terminal Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Terminal Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAYMENT TERMINAL",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color(0xFFF8FAFC)
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                androidx.compose.ui.graphics.Color(0xFF10B981),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ONLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Amount Display (POS Style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Color(0xFF0F172A),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AMOUNT",
                            fontSize = 12.sp,
                            color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                        )
                        Text(
                            text = "₹10.00",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Instructions
                Spacer(modifier = Modifier.height(20.dp))

                // Instructions
                Text(
                    text = "Tap to Pay or Play audio",
                    fontSize = 16.sp,
                    color = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Start Payment Button (POS Style)
                Button(
                    onClick = {
                        Log.d("Debug", "Start button CLICKED!")
                        narrator.speak("Starting Simulation, Initiating the transaction of 10 Rupees")
                        startTransaction(onCardsLoaded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF2563EB)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "START PAYMENT SIMULATION",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Methods Accepted
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PaymentMethodChip("💳 Cards")
                    PaymentMethodChip("📱 NFC")
                    PaymentMethodChip("🎵 Audio")
                }
            }
        }
    }

    @Composable
    fun PaymentMethodChip(text: String) {
        Box(
            modifier = Modifier
                .background(
                    androidx.compose.ui.graphics.Color(0xFF334155),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
            )
        }
    }

    @Composable
    fun AudioDemoSection() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Audio Communication",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Automatically listening for ultrasound messages",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status: ${AudioStatus.value}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status section - Always listening mode
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (AudioStatus.value == "Ready" && ggWaveListening.value)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (AudioStatus.value == "Ready" && ggWaveListening.value)
                                "🎤 Always Listening"
                            else "⏳ Starting Listener...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (AudioStatus.value == "Ready" && ggWaveListening.value)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Automatically listening for ultrasound messages",
                            fontSize = 12.sp,
                            color = if (AudioStatus.value == "Ready" && ggWaveListening.value)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }



                // Received Messages Display - Always visible
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📨 Received Ultrasound Messages",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${ggWaveReceivedMessages.value.size} msgs",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Scrollable text box for received messages
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            if (ggWaveReceivedMessages.value.isEmpty()) {
                                // Show placeholder when no messages
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "🎧",
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Listening for ultrasound messages...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            } else {
                                // Show received messages
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    ggWaveReceivedMessages.value.reversed().forEach { message ->
                                        Text(
                                            text = "• $message",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Test message button for demonstration
                            Button(
                                onClick = { simulateUltrasoundMessage() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            ) {
                                Text("Test Message", fontSize = 12.sp)
                            }

                            // Debug button for troubleshooting
                            Button(
                                onClick = { showAudioDebugInfo() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                                )
                            ) {
                                Text("Debug Audio", fontSize = 12.sp)
                            }

                            // Clear messages button - only show if there are messages
                            if (ggWaveReceivedMessages.value.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        ggWaveReceivedMessages.value = emptyList()
                                        ggWaveLastMessage.value = ""
                                        addGGWaveLog("🗑️ Cleared received messages")
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text("Clear", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SavedCardsSection(cards: List<Card>, onCardClick: (Card) -> Unit) {
        Log.d(
            "Debug",
            "SavedCardsSection: FUNCTION ENTRY - COMPOSING with ${cards.size} cards"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // POS-style header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CUSTOMER PAYMENT CARDS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFFF8FAFC)
                        )
                        Text(
                            text = "${cards.size} cards available",
                            fontSize = 12.sp,
                            color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                androidx.compose.ui.graphics.Color(0xFF3B82F6),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SELECT CARD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // POS-style card list
                cards.forEachIndexed { index, card ->
                    Log.d("Debug", "Creating UI for card $index: ****${card.last4Digits}")

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF334155)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Card Type Icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            androidx.compose.ui.graphics.Color(0xFF1E293B),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "💳",
                                        fontSize = 20.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = card.issuerBank.toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = androidx.compose.ui.graphics.Color(0xFFF8FAFC)
                                    )
                                    Text(
                                        text = "•••• •••• •••• ${card.last4Digits}",
                                        fontSize = 14.sp,
                                        color = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "${card.network} ${card.cardType}",
                                        fontSize = 12.sp,
                                        color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    Log.d("Debug", "Card clicked: ****${card.last4Digits}")
                                    onCardClick(card)
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFF059669)
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "PAY\n₹10",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    if (index < cards.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // POS-style hide button
                Button(
                    onClick = {
                        Log.d("Debug", "Hide Cards clicked")
                        showSavedCards = false
                        savedCardsList = emptyList()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF475569)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "HIDE CARDS",
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }

    // 🚫 REMOVED: ThreeDSWebViewSection - Using fullscreen activity only!

    @Composable
    fun PaymentResponseSection(response: String, onClear: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Payment Response",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = response,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp),
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Show reopen button if 3DS authentication was initiated
                if (lastThreeDSContent.isNotEmpty()) {
                    Button(
                        onClick = {
                            Log.d("Test", "Reopening 3DS authentication")
                            ThreeDSWebViewActivity.start(
                                this@MainActivity,
                                lastThreeDSContent,
                                lastPaymentId
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Reopen 3DS Authentication",
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = onClear,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Clear Response",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
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
                setAutoSimulation(true, 2000) // Auto-simulate after 3 seconds
            }

            // Initialize the POS NFC Device Manager with PAX plugin
            nfcManager = PosNfcDeviceManager(this, paxPlugin)

            // Initialize cards service
            cardsService = ListSavedCards()

            // Initialize payment service
            Log.d("MainActivity", "Creating InitiatePayment instance...")
            paymentService = InitiatePayment(this@MainActivity)
            Log.d("MainActivity", "InitiatePayment instance created successfully")

            // Initialize GGWave with auto volume adjustment
            ggWave = GGWaveImpl(this, true)
            AudioStatus.value = "Initialized"
            addGGWaveLog("🔊 GGWave audio communication initialized")

            // Initialize GGWave WebView and make it ready for use
            initializeGGWaveOnAppLoad()

            sdkStatus.value = "Initialized Successfully"
            pluginMode.value = "Mock Mode"
            addLog("✅ DrishtiPay POS SDK initialized successfully!")
            addLog("📱 Ready for emulator testing with mock NFC simulation")
            addLog("🎵 GGWave audio communication ready")

        } catch (e: Exception) {
            sdkStatus.value = "Initialization Failed"
            addLog("❌ Failed to initialize DrishtiPay SDK: ${e.message}")
            Log.e("MainActivity", "Failed to initialize DrishtiPay SDK", e)
        }
    }

    private fun initializeDrishtiPaySDKWithoutGGWave() {
        try {
            addLog("Initializing DrishtiPay POS SDK (without GGWave)...")

            // Create PAX plugin in mock mode by default
            paxPlugin = PaxNeptuneLitePlugin().apply {
                setMockMode(true)
                setAutoSimulation(true, 2000) // Auto-simulate after 3 seconds
            }

            // Initialize the POS NFC Device Manager with PAX plugin
            nfcManager = PosNfcDeviceManager(this, paxPlugin)

            // Initialize cards service
            cardsService = ListSavedCards()

            // Initialize payment service
            Log.d("MainActivity", "Creating InitiatePayment instance...")
            paymentService = InitiatePayment(this@MainActivity)
            Log.d("MainActivity", "InitiatePayment instance created successfully")

            // Skip GGWave initialization due to missing permission
            AudioStatus.value = "Permission Denied"
            addLog("⚠️ GGWave initialization skipped - audio permission required")

            sdkStatus.value = "Initialized Successfully"
            pluginMode.value = "Mock Mode"
            addLog("✅ DrishtiPay POS SDK initialized successfully!")
            addLog("📱 Ready for emulator testing with mock NFC simulation")
            addLog("⚠️ GGWave unavailable - audio permission required")

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
                        Toast.makeText(
                            this@MainActivity,
                            "NFC Error: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
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

            // Call the saved cards API with configuration from BuildConfig
            val merchantId = BuildConfig.MERCHANT_ID
            val contact = BuildConfig.TEST_CONTACT

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

            lastPaymentData.value =
                "SAVED CARDS TEST:\n\n$cardsInfo\n\n✅ Mock cards loaded successfully!"

            Toast.makeText(
                this,
                "Saved Cards loaded: ${cardsInfo.split("\n").size - 3} cards",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            addLog("❌ Error testing saved cards: ${e.message}")
            Toast.makeText(this, "Error loading saved cards: ${e.message}", Toast.LENGTH_LONG)
                .show()
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
            Toast.makeText(this, "Payment processing failed: ${e.message}", Toast.LENGTH_LONG)
                .show()
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

    // todo
    private fun startTransaction(onCardsLoaded: (List<Card>, Boolean) -> Unit) {
        Log.d("Debug", "🔥 startTest: FUNCTION CALLED!")
        addLog("🚀  Start button clicked!")

        try {
            Log.d("Debug", "🔥 startTest: Starting card loading process")
            addLog("📋 Loading saved cards...")

            if (!::cardsService.isInitialized) {
                Log.e("Debug", "🔥 ERROR: cardsService not initialized!")
                addLog("❌ cardsService not initialized!")
                return
            }

            Log.d("Debug", "🔥 cardsService is initialized, proceeding...")

            val merchantId = "CO9vBE2ZlgawZYVDx2Y9"

            //todo fix this. this will not be hardcoded.
            val contact = "+918955496900"

            Log.d(
                "Debug",
                "🔥 Calling listAllSavedCards with merchantId: $merchantId, contact: $contact"
            )
            val savedCardsResponse = cardsService.listAllSavedCards(merchantId, contact)
            Log.d("Debug", "🔥 Got savedCardsResponse with ${savedCardsResponse.size} items")

            val cards = savedCardsResponse.flatMap { it.cards.toList() }
            Log.d("Debug", "🔥 Extracted ${cards.size} cards from response")

            cards.forEachIndexed { index, card ->
                Log.d(
                    "Debug",
                    "🔥 Card $index: ID=${card.cardId}, Last4=${card.last4Digits}, Network=${card.network}"
                )
            }

            Log.d("Debug", "🔥 About to call onCardsLoaded callback with ${cards.size} cards")

            // Force UI update on main thread
            runOnUiThread {
                onCardsLoaded(cards, true)
                Log.d("Debug", "🔥 onCardsLoaded callback completed on UI thread!")
            }

            addLog("✅ Loaded ${cards.size} saved cards")
            addLog("💳 Displaying cards for selection...")

            Toast.makeText(this, "Loaded ${cards.size} cards successfully!", Toast.LENGTH_SHORT)
                .show()

        } catch (e: Exception) {
            Log.e("Debug", "🔥 ERROR in startTest: ${e.message}", e)
            addLog("❌ Error in  test: ${e.message}")
            Toast.makeText(this, "Error loading cards: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initiatePaymentForCard(card: Card, onResponse: (String) -> Unit) {
        Log.d("Test", "💰 Initiating payment for card ****${card.last4Digits}")
        addLog("💰 Initiating payment for card ****${card.last4Digits}")

        if (!::paymentService.isInitialized) {
            Log.e("Test", "❌ paymentService not initialized!")
            addLog("❌ paymentService not initialized!")
            return
        }

        // Run payment on background thread
        Thread {
            try {
                Log.d("Test", "🔄 Processing payment...")
                addLog("🔄 Processing payment...")

                val response = paymentService.initiatePayment(card, 10.0f)
                Log.d("Test", "Payment response received: $response")

                runOnUiThread {
                    if (response != null) {
                        val acsUrl = response.acsURL ?: ""
                        val paymentId = response.paymentId ?: ""
                        val orderId = response.orderId ?: ""

                        // Check if acsURL contains HTML (3DS authentication required)
                        if (acsUrl.contains("<html") || acsUrl.contains("<!DOCTYPE") || acsUrl.contains(
                                "<form"
                            ) || acsUrl.length > 500
                        ) {
                            Log.d(
                                "Test",
                                "🔥🌐 HTML response detected in acsURL - 3DS authentication required!"
                            )
                            Log.d("Test", "🔥🌐 HTML Content Preview: ${acsUrl.take(200)}...")
                            addLog("🔐 3DS Authentication required - opening WebView")

                            // 🔥 LAUNCH FULLSCREEN WEBVIEW ONLY - No inline backup
                            Log.d("Test", "🔥🌐 Launching fullscreen 3DS WebView activity ONLY")
                            ThreeDSWebViewActivity.start(this@MainActivity, acsUrl, paymentId)

                            // Show response in payment section with reopen option
                            onResponse("3DS Authentication opened in fullscreen mode\n\nHTML detected (${acsUrl.length} chars)\n\nComplete authentication in the new screen\n\nIf closed accidentally, you can find 'Reopen 3DS Authentication' button below")

                            Toast.makeText(
                                this@MainActivity,
                                "3DS Authentication opened in fullscreen!",
                                Toast.LENGTH_LONG
                            ).show()

                            // Store HTML for potential reopen
                            lastThreeDSContent = acsUrl
                            lastPaymentId = paymentId

                            // 🚫 DO NOT SET INLINE WEBVIEW - Keep it clean!
                            Log.d(
                                "Test",
                                "🔥🌐 NOT setting inline WebView variables - fullscreen only"
                            )
                        } else {
                            // Regular JSON response
                            val responseText = """
                                Payment Initiated Successfully
                                
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

                            Toast.makeText(
                                this@MainActivity,
                                "Payment processed! Check response above",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        onResponse("❌ Payment failed - no response received")
                        addLog("❌ Payment failed - null response")
                        Log.e("Test", "❌ Payment failed - null response")
                    }
                }

            } catch (e: Exception) {
                Log.e("Test", "❌ Payment error: ${e.message}", e)
                runOnUiThread {
                    onResponse("❌ Payment Error: ${e.message}")
                    addLog("❌ Payment error: ${e.message}")
                    Toast.makeText(
                        this@MainActivity,
                        "Payment error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    // ===================== GGWave Functions =====================

    private fun initializeGGWaveOnAppLoad() {
        if (!::ggWave.isInitialized) {
            addGGWaveLog("⚠️ GGWave not initialized - permission required")
            return
        }

        try {
            addGGWaveLog("🔧 Initializing GGWave WebView...")
            ggWave.initialize {
                runOnUiThread {
                    AudioStatus.value = "Ready"
                    addGGWaveLog("✅ GGWave initialized and ready!")
                    addLog("🎵 GGWave is ready for audio communication")

                    // Automatically start listening in background
                    startBackgroundListening()
                }
            }
        } catch (e: Exception) {
            AudioStatus.value = "Error"
            addGGWaveLog("❌ GGWave initialization failed: ${e.message}")
            addLog("❌ GGWave initialization failed: ${e.message}")
        }
    }

    private fun initializeGGWave() {
        // GGWave is now auto-initialized on app load
        if (AudioStatus.value == "Ready") {
            addGGWaveLog("ℹ️ GGWave already initialized and ready!")
            Toast.makeText(this@MainActivity, " Audio transfer Already Ready", Toast.LENGTH_SHORT).show()
        } else {
            addGGWaveLog("🔄 Re-initializing GGWave...")
            initializeGGWaveOnAppLoad()
        }
    }

    private fun simulateUltrasoundMessage() {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val testMessage = "📱 Test Message from +91-9876543210 | App: DrishtiPay"
        val timestampedMessage = "[$timestamp] $testMessage"

        ggWaveLastMessage.value = testMessage
        ggWaveReceivedMessages.value = ggWaveReceivedMessages.value + timestampedMessage

        addGGWaveLog("🧪 Simulated ultrasound message for testing")
        Toast.makeText(this@MainActivity, "Test ultrasound message received!", Toast.LENGTH_SHORT).show()
    }

    private fun showAudioDebugInfo() {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

            // Check microphone availability
            val hasAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val hasSystemFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

            // Audio settings
            val micMuted = audioManager.isMicrophoneMute
            val mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxMediaVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            // Check GGWave status
            val isGGWaveInitialized = ::ggWave.isInitialized && ggWave.isInitialized()
            val isGGWaveListening = if (isGGWaveInitialized) ggWave.isListening() else false

            val debugInfo = """
🔍 Audio Debug Information:
✅ Audio Permission: ${if (hasAudioPermission) "Granted" else "DENIED"}
✅ Microphone Feature: ${if (hasSystemFeature) "Available" else "NOT AVAILABLE"}
🎤 Microphone Muted: ${if (micMuted) "YES - ISSUE!" else "No"}
🔊 Media Volume: $mediaVolume/$maxMediaVolume
📱 GGWave Initialized: ${if (isGGWaveInitialized) "Yes" else "NO - ISSUE!"}
🎧 GGWave Listening: ${if (isGGWaveListening) "Yes" else "NO - ISSUE!"}

💡 Common Issues:
${if (micMuted) "• Microphone is muted - unmute to receive audio\n" else ""}
${if (!hasAudioPermission) "• Missing audio permission\n" else ""}
${if (!isGGWaveListening) "• GGWave not listening - restart app\n" else ""}
${if (mediaVolume == 0) "• Media volume is 0 - increase volume\n" else ""}

🎵 To test: Play ultrasound from another device
📡 Emulator Note: Audio input may have limitations
            """.trimIndent()

            addGGWaveLog("🔍 Audio debug info generated")
            addLog("🔍 Audio Debug Complete - Check details")

            Toast.makeText(this, "Debug info logged - check logs section", Toast.LENGTH_LONG).show()

            // Also log to Android logs for detailed debugging
            Log.d("AudioDebug", debugInfo)

        } catch (e: Exception) {
            addGGWaveLog("❌ Error getting audio debug info: ${e.message}")
            Toast.makeText(this, "Error getting debug info: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startBackgroundListening() {
        if (ggWaveListening.value) {
            addGGWaveLog("ℹ️ Already listening in background")
            return
        }

        try {
            addGGWaveLog("🎧 Starting background listening...")
            val success = ggWave.startListening(object : IGGWave.GGWaveCallback {
                override fun onMessageReceived(message: GGWaveMessage): Boolean {
                    runOnUiThread {
                        // Add timestamp to received structured message
                        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        val displayMessage = "📱 Mobile: ${message.mobileNumber} | App: ${message.appType}"
                        val timestampedMessage = "[$timestamp] $displayMessage"

                        // Update last message and add to received messages list
                        ggWaveLastMessage.value = displayMessage
                        ggWaveReceivedMessages.value = ggWaveReceivedMessages.value + timestampedMessage

                        addGGWaveLog("📨 DrishtiPay Message - Mobile: [REDACTED] | App: ${message.appType}")
                        narrator.speak("Customer detected, loading payment options")
                        Toast.makeText(this@MainActivity, "Customer Ready for Payment!", Toast.LENGTH_SHORT).show()

                        // Execute the same functionality as "Start Simulation" button click
                        Log.d("Debug", "Start button CLICKED!")
                        narrator.speak("Starting Simulation, Initiating the transaction of 10 Rupees")

                        // Trigger the transaction functionality exactly like the Start Simulation button
                        startTransaction { newCards, showCards ->
                            Log.d("Debug", "Audio triggered cards callback: ${newCards.size} cards, show = $showCards")

                            // Update class-level state variables that can be accessed by Composables
                            CardsState.value = newCards
                            ShowCardsState.value = showCards

                            Log.d("Debug", "Updated CardsState with ${CardsState.value.size} cards")
                            Log.d("Debug", "Updated ShowCardsState to $showCards")
                        }
                    }
                    return true
                }

                override fun onRawMessageReceived(rawMessage: String): Boolean {
                    runOnUiThread {
                        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        val timestampedMessage = "[$timestamp] Raw: $rawMessage"

                        ggWaveReceivedMessages.value = ggWaveReceivedMessages.value + timestampedMessage
                        addGGWaveLog("📨 Raw message: $rawMessage")

                        // Execute the same functionality as "Start Simulation" button click for raw messages too
                        Log.d("Debug", "Start button CLICKED!")
                        narrator.speak("Starting Simulation, Initiating the transaction of 10 Rupees")

                        // Trigger the transaction functionality exactly like the Start Simulation button
                        startTransaction { newCards, showCards ->
                            Log.d("Debug", "Raw message triggered cards callback: ${newCards.size} cards, show = $showCards")

                            // Update class-level state variables that can be accessed by Composables
                            CardsState.value = newCards
                            ShowCardsState.value = showCards

                            Log.d("Debug", "Updated CardsState with ${CardsState.value.size} cards")
                            Log.d("Debug", "Updated ShowCardsState to $showCards")
                        }
                    }
                    return true
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        addGGWaveLog("❌ GGWave error: $error")
                        Toast.makeText(this@MainActivity, "Audio transfer Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            })

            if (success) {
                ggWaveListening.value = true
                addGGWaveLog("✅ Background listening started successfully")
                addLog("🎧 GGWave now listening in background for messages")
            } else {
                addGGWaveLog("❌ Failed to start background listening")
            }
        } catch (e: Exception) {
            addGGWaveLog("❌ Error starting background listening: ${e.message}")
        }
    }

    private fun startGGWaveListening() {
        if (ggWaveListening.value) {
            // Stop listening
            try {
                ggWave.stopListening()
                ggWaveListening.value = false
                addGGWaveLog("🔇 GGWave listening stopped")
            } catch (e: Exception) {
                addGGWaveLog("❌ Error stopping GGWave: ${e.message}")
            }
        } else {
            // Start listening
            try {
                val success = ggWave.startListening(object : IGGWave.GGWaveCallback {
                    override fun onMessageReceived(message: GGWaveMessage): Boolean {
                        runOnUiThread {
                            // Add timestamp to received structured message
                            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date())
                            val displayMessage = "📱 Mobile: ${message.mobileNumber} | App: ${message.appType}"
                            val timestampedMessage = "[$timestamp] $displayMessage"

                            // Update last message and add to received messages list
                            ggWaveLastMessage.value = displayMessage
                            ggWaveReceivedMessages.value = ggWaveReceivedMessages.value + timestampedMessage

                            addGGWaveLog("📨 DrishtiPay Message - Mobile: [REDACTED] | App: ${message.appType}")
                            narrator.speak("Customer detected, loading payment options")
                            Toast.makeText(this@MainActivity, "Customer Ready for Payment!", Toast.LENGTH_SHORT).show()

                            // Execute the same functionality as "Start Simulation" button click
                            Log.d("Debug", "Start button CLICKED!")
                            narrator.speak("Starting Simulation, Initiating the transaction of 10 Rupees")

                            // Trigger the transaction functionality exactly like the Start Simulation button
                            startTransaction { newCards, showCards ->
                                Log.d("Debug", "Audio triggered cards callback: ${newCards.size} cards, show = $showCards")

                                // Update class-level state variables that can be accessed by Composables
                                CardsState.value = newCards
                                ShowCardsState.value = showCards
                                ForceRefresh.value = ForceRefresh.value + 1

                                Log.d("Debug", "Cards state updated: ${CardsState.value.size} cards, show = ${ShowCardsState.value}")
                            }
                        }
                        return true // Continue listening
                    }

                    override fun onRawMessageReceived(rawMessage: String): Boolean {
                        runOnUiThread {
                            // Add timestamp to received raw message
                            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date())
                            val timestampedMessage = "[$timestamp] Raw: $rawMessage"

                            // Update last message and add to received messages list
                            ggWaveLastMessage.value = "Raw: $rawMessage"
                            ggWaveReceivedMessages.value = ggWaveReceivedMessages.value + timestampedMessage

                            addGGWaveLog("📨 Raw message: $rawMessage")
                            narrator.speak("Customer detected, loading payment options")
                            Toast.makeText(this@MainActivity, "Customer Ready for Payment!", Toast.LENGTH_SHORT).show()

                            // Execute the same functionality as "Start Simulation" button click for raw messages too
                            Log.d("Debug", "Start button CLICKED!")
                            narrator.speak("Starting Simulation, Initiating the transaction of 10 Rupees")

                            // Trigger the transaction functionality exactly like the Start Simulation button
                            startTransaction { newCards, showCards ->
                                Log.d("Debug", "Raw message triggered cards callback: ${newCards.size} cards, show = $showCards")

                                // Update class-level state variables that can be accessed by Composables
                                CardsState.value = newCards
                                ShowCardsState.value = showCards
                                ForceRefresh.value = ForceRefresh.value + 1

                                Log.d("Debug", "Cards state updated: ${CardsState.value.size} cards, show = ${ShowCardsState.value}")
                            }
                        }
                        return true // Continue listening
                    }

                    override fun onError(error: String) {
                        runOnUiThread {
                            addGGWaveLog("❌ Receive error: $error")
                        }
                    }
                })
                if (success) {
                    ggWaveListening.value = true
                    addGGWaveLog("🎤 GGWave listening started")
                } else {
                    addGGWaveLog("❌ Failed to start audio based listening")
                }
            } catch (e: Exception) {
                addGGWaveLog("❌ Error starting audio: ${e.message}")
            }
        }
    }

    private fun sendGGWaveTestMessage() {
        try {
            // Create structured DrishtiPay message with test mobile number
            val testMobileNumber = "9348192478192" // From user's example
            addGGWaveLog("📤 Sending DrishtiPay message via ULTRASOUND with mobile: [REDACTED]")

            // Create structured message and send with ultrasound mode
            val customMessage = GGWaveMessage(testMobileNumber, "drishtipay_app", "ggwave")
            val success = ggWave.sendMessage(customMessage, true, true, object : IGGWave.GGWaveTransmissionCallback {
                override fun onTransmissionComplete() {
                    runOnUiThread {
                        addGGWaveLog("✅ DrishtiPay ULTRASOUND message sent successfully!")
                        narrator.speak("DrishtiPay ultrasound message sent")
                        Toast.makeText(this@MainActivity, "DrishtiPay ultrasound message sent!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onTransmissionError(error: String) {
                    runOnUiThread {
                        addGGWaveLog("❌ Ultrasound send error: $error")
                        Toast.makeText(this@MainActivity, "Send failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
            })

            if (success) {
                addGGWaveLog("✅ DrishtiPay ULTRASOUND transmission started")
            } else {
                addGGWaveLog("❌ Failed to start DrishtiPay ultrasound transmission")
            }
        } catch (e: Exception) {
            addGGWaveLog("❌ Error sending DrishtiPay message: ${e.message}")
        }
    }

    private fun addGGWaveLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logEntry = "[$timestamp] $message"

        ggWaveLogMessages.value = ggWaveLogMessages.value + logEntry
        Log.d("audio-transfer", message)
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

            // Clean up GGWave resources
            if (::ggWave.isInitialized) {
                ggWave.cleanup()
            }

            // Clean up narrator
            if (::narrator.isInitialized) {
                narrator.shutdown()
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