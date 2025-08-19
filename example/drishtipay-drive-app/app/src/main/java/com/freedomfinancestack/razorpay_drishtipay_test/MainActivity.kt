@file:OptIn(ExperimentalMaterial3Api::class)

package com.freedomfinancestack.razorpay_drishtipay_test

import android.Manifest
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
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
import com.freedomfinancestack.pos_sdk_core.implementations.SoundDataTransmissionImpl
import com.freedomfinancestack.pos_sdk_core.interfaces.ISoundDataTransmission
import com.freedomfinancestack.razorpay_drishtipay_test.payment.InitiatePayment
import com.freedomfinancestack.razorpay_drishtipay_test.pos.PaxNeptuneLitePlugin
import com.freedomfinancestack.razorpay_drishtipay_test.savedcards.ListSavedCards
import com.freedomfinancestack.razorpay_drishtipay_test.ui.theme.RazorpaydrishtipaytestTheme

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 1001
    }
    
    private lateinit var nfcManager: INfcDeviceManager
    private lateinit var paxPlugin: PaxNeptuneLitePlugin
    private lateinit var cardsService: ListSavedCards
    private lateinit var paymentService: InitiatePayment
    private lateinit var narrator: NarratorImpl
    private lateinit var soundTransmission: ISoundDataTransmission
    private var isListening = mutableStateOf(false)
    private var sdkStatus = mutableStateOf("Not Initialized")
    private var lastPaymentData = mutableStateOf("No payments processed")
    private var pluginMode = mutableStateOf("Mock Mode")
    private var logMessages = mutableStateOf(listOf<String>())
    
    // Sound transmission debug states
    private var soundTransmissionText = mutableStateOf("Hello from POS!")
    private var soundTransmissionStatus = mutableStateOf("Ready")
    private var soundReceivedData = mutableStateOf("")
    private var isSoundListening = mutableStateOf(false)

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
        
        // Initialize DrishtiPay POS SDK
        initializeDrishtiPaySDK()
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
                TopAppBar(
                    title = {
                        Text(
                            text = "DrishtiPay POS SDK",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets
                )
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
                VarunTestSection { newCards, showCards ->
                    Log.d(
                        "VarunDebug",
                        "Callback received: ${newCards.size} cards, show = $showCards"
                    )
                    savedCardsState = newCards
                    showSavedCardsState = showCards
                    forceRefresh++ // Force recomposition
                    Log.d(
                        "VarunDebug",
                        "State updated: savedCardsState = ${savedCardsState.size}, showSavedCardsState = $showSavedCardsState"
                    )
                }

                // Saved Cards Section (only show when cards are loaded)
                if (showSavedCardsState) {
                    Log.d(
                        "VarunDebug",
                        "SHOWING SavedCardsSection with ${savedCardsState.size} cards!"
                    )
                    SavedCardsSection(savedCardsState) { card ->
                        Log.d("VarunDebug", "Card clicked: ${card.last4Digits}")
                        initiatePaymentForCard(card) { response ->
                            Log.d("VarunDebug", "Payment response received")
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

                // Sound Transmission Debug Section
                SoundTransmissionDebugSection()

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
    fun SoundTransmissionDebugSection() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔊 Sound Transmission Debug",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                // Status
                Text(
                    text = "Status: ${soundTransmissionStatus.value}",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                // Input text field for data to send
                OutlinedTextField(
                    value = soundTransmissionText.value,
                    onValueChange = { soundTransmissionText.value = it },
                    label = { Text("Data to send via sound") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter message to transmit...") }
                )
                
                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Send button
                    Button(
                        onClick = { sendSoundData() },
                        modifier = Modifier.weight(1f),
                        enabled = soundTransmissionStatus.value == "Initialized" || soundTransmissionStatus.value == "Ready"
                    ) {
                        Text("Send Data")
                    }
                    
                    // Listen/Stop button
                    Button(
                        onClick = { 
                            if (isSoundListening.value) {
                                stopSoundListening()
                            } else {
                                startSoundListening()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSoundListening.value) 
                                MaterialTheme.colorScheme.error 
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isSoundListening.value) "Stop Listen" else "Start Listen")
                    }
                }
                
                // Received data display
                if (soundReceivedData.value.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "📥 Received Data:",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = soundReceivedData.value,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "NFC Payment Simulation",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Load saved cards and simulate payment flow",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        Log.d("VarunDebug", "Start button CLICKED!")
                        narrator.speak("Hello my name is Varun Bansal")
                        startVarunTest(onCardsLoaded)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Start Simulation",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "• Load saved payment cards\n• Display cards for selection\n• Process payment on card selection",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }

    @Composable
    fun SavedCardsSection(cards: List<Card>, onCardClick: (Card) -> Unit) {
        Log.d(
            "VarunDebug",
            "SavedCardsSection: FUNCTION ENTRY - COMPOSING with ${cards.size} cards"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Professional header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Saved Payment Cards",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${cards.size} cards available",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Professional card list
                cards.forEachIndexed { index, card ->
                    Log.d("VarunDebug", "Creating UI for card $index: ****${card.last4Digits}")

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = card.issuerBank.toString(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "•••• •••• •••• ${card.last4Digits}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${card.network} ${card.cardType}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    Log.d("VarunDebug", "Card clicked: ****${card.last4Digits}")
                                    onCardClick(card)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = "Pay ₹10",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (index < cards.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Professional hide button
                Button(
                    onClick = {
                        Log.d("VarunDebug", "Hide Cards clicked")
                        showSavedCards = false
                        savedCardsList = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Hide Cards",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
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
                            Log.d("VarunTest", "Reopening 3DS authentication")
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
                setAutoSimulation(true, 3000) // Auto-simulate after 3 seconds
            }

            // Initialize the POS NFC Device Manager with PAX plugin
            nfcManager = PosNfcDeviceManager(this, paxPlugin)

            // Initialize cards service
            cardsService = ListSavedCards()

            // Initialize payment service
            paymentService = InitiatePayment()

            // Initialize sound transmission
            soundTransmission = SoundDataTransmissionImpl(this)
            soundTransmissionStatus.value = "Initialized"

            sdkStatus.value = "Initialized Successfully"
            pluginMode.value = "Mock Mode"
            addLog("✅ DrishtiPay POS SDK initialized successfully!")
            addLog("🔊 Sound transmission API ready!")
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

            Log.d(
                "VarunDebug",
                "🔥 Calling listAllSavedCards with merchantId: $merchantId, contact: $contact"
            )
            val savedCardsResponse = cardsService.listAllSavedCards(merchantId, contact)
            Log.d("VarunDebug", "🔥 Got savedCardsResponse with ${savedCardsResponse.size} items")

            val cards = savedCardsResponse.flatMap { it.cards.toList() }
            Log.d("VarunDebug", "🔥 Extracted ${cards.size} cards from response")

            cards.forEachIndexed { index, card ->
                Log.d(
                    "VarunDebug",
                    "🔥 Card $index: ID=${card.cardId}, Last4=${card.last4Digits}, Network=${card.network}"
                )
            }

            Log.d("VarunDebug", "🔥 About to call onCardsLoaded callback with ${cards.size} cards")

            // Force UI update on main thread
            runOnUiThread {
                onCardsLoaded(cards, true)
                Log.d("VarunDebug", "🔥 onCardsLoaded callback completed on UI thread!")
            }

            addLog("✅ Loaded ${cards.size} saved cards")
            addLog("💳 Displaying cards for selection...")

            Toast.makeText(this, "Loaded ${cards.size} cards successfully!", Toast.LENGTH_SHORT)
                .show()

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
                        if (acsUrl.contains("<html") || acsUrl.contains("<!DOCTYPE") || acsUrl.contains(
                                "<form"
                            ) || acsUrl.length > 500
                        ) {
                            Log.d(
                                "VarunTest",
                                "🔥🌐 HTML response detected in acsURL - 3DS authentication required!"
                            )
                            Log.d("VarunTest", "🔥🌐 HTML Content Preview: ${acsUrl.take(200)}...")
                            addLog("🔐 3DS Authentication required - opening WebView")

                            // 🔥 LAUNCH FULLSCREEN WEBVIEW ONLY - No inline backup
                            Log.d("VarunTest", "🔥🌐 Launching fullscreen 3DS WebView activity ONLY")
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
                                "VarunTest",
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
                        Log.e("VarunTest", "❌ Payment failed - null response")
                    }
                }

            } catch (e: Exception) {
                Log.e("VarunTest", "❌ Payment error: ${e.message}", e)
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
    
    // Sound transmission functions
    private fun sendSoundData() {
        try {
            // Check for RECORD_AUDIO permission first
            if (!hasRecordAudioPermission()) {
                requestRecordAudioPermission()
                return
            }
            
            val dataToSend = soundTransmissionText.value.trim()
            if (dataToSend.isEmpty()) {
                Toast.makeText(this, "Please enter some data to send", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (dataToSend.length > 140) {
                Toast.makeText(this, "Data too long! Maximum 140 characters", Toast.LENGTH_SHORT).show()
                return
            }
            
            soundTransmissionStatus.value = "Sending..."
            addLog("🔊 Sending via sound: $dataToSend")
            
            soundTransmission.send(dataToSend, object : ISoundDataTransmission.SoundCallback {
                override fun onReceived(data: String) {
                    // Not used when sending
                }
                
                override fun onSent(data: String) {
                    runOnUiThread {
                        soundTransmissionStatus.value = "Sent successfully!"
                        addLog("✅ Sound data sent: $data")
                        Toast.makeText(this@MainActivity, "Data sent via sound!", Toast.LENGTH_SHORT).show()
                        
                        // Reset status after a delay
                        android.os.Handler(mainLooper).postDelayed({
                            soundTransmissionStatus.value = "Ready"
                        }, 2000)
                    }
                }
                
                override fun onError(error: String) {
                    runOnUiThread {
                        soundTransmissionStatus.value = "Send failed"
                        addLog("❌ Sound transmission error: $error")
                        Toast.makeText(this@MainActivity, "Send failed: $error", Toast.LENGTH_LONG).show()
                        
                        // Reset status after a delay
                        android.os.Handler(mainLooper).postDelayed({
                            soundTransmissionStatus.value = "Ready"
                        }, 3000)
                    }
                }
            })
        } catch (e: Exception) {
            soundTransmissionStatus.value = "Error"
            addLog("❌ Failed to send sound data: ${e.message}")
            Log.e("MainActivity", "Failed to send sound data", e)
        }
    }
    
    private fun startSoundListening() {
        try {
            // Check for RECORD_AUDIO permission first
            if (!hasRecordAudioPermission()) {
                requestRecordAudioPermission()
                return
            }
            
            isSoundListening.value = true
            soundTransmissionStatus.value = "Listening..."
            addLog("👂 Started listening for sound data...")
            
            soundTransmission.listen(object : ISoundDataTransmission.SoundCallback {
                override fun onReceived(data: String) {
                    runOnUiThread {
                        soundReceivedData.value = data
                        addLog("📥 Received via sound: $data")
                        Toast.makeText(this@MainActivity, "Received: $data", Toast.LENGTH_LONG).show()
                        
                        // Speak the received data
                        if (::narrator.isInitialized) {
                            narrator.speak("Received data: $data")
                        }
                    }
                }
                
                override fun onSent(data: String) {
                    // Not used when listening
                }
                
                override fun onError(error: String) {
                    runOnUiThread {
                        addLog("❌ Sound listening error: $error")
                        Toast.makeText(this@MainActivity, "Listen error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: Exception) {
            isSoundListening.value = false
            soundTransmissionStatus.value = "Error"
            addLog("❌ Failed to start sound listening: ${e.message}")
            Log.e("MainActivity", "Failed to start sound listening", e)
        }
    }
    
    private fun stopSoundListening() {
        try {
            isSoundListening.value = false
            soundTransmissionStatus.value = "Ready"
            addLog("🔇 Stopped sound listening")
            
            soundTransmission.stop()
            
            Toast.makeText(this, "Stopped listening", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            addLog("❌ Failed to stop sound listening: ${e.message}")
            Log.e("MainActivity", "Failed to stop sound listening", e)
        }
    }
    
    // Permission handling functions
    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            RECORD_AUDIO_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addLog("🎤 Audio permission granted!")
                    Toast.makeText(this, "Audio permission granted. You can now use sound transmission.", Toast.LENGTH_LONG).show()
                    soundTransmissionStatus.value = "Ready"
                } else {
                    addLog("❌ Audio permission denied!")
                    Toast.makeText(this, "Audio permission required for sound transmission", Toast.LENGTH_LONG).show()
                    soundTransmissionStatus.value = "Permission Required"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up SDK resources
        try {
            // Clean up sound transmission
            if (::soundTransmission.isInitialized) {
                soundTransmission.stop()
            }
            
            if (::nfcManager.isInitialized) {
                nfcManager.stopListening()
                if (nfcManager is PosNfcDeviceManager) {
                    (nfcManager as PosNfcDeviceManager).cleanup()
                }
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