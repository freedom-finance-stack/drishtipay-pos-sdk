# DrishtiPay POS SDK - Complete Integration Guide

## 📋 Table of Contents
- [Overview](#overview)
- [What You'll Build](#what-youll-build)
- [Prerequisites](#prerequisites)
- [Quick Start (5 Minutes)](#quick-start-5-minutes)
- [Detailed Setup](#detailed-setup)
- [Core Features](#core-features)
- [Code Examples](#code-examples)
- [Advanced Configuration](#advanced-configuration)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [API Reference](#api-reference)
- [Best Practices](#best-practices)

---

## Overview

**DrishtiPay POS SDK** is a universal Android library that enables **contactless payments** on Point-of-Sale (POS) devices. It supports both **NFC tap-to-pay** and **audio-based communication** (GGWave) for maximum compatibility.

### ✨ Key Features
- 🏪 **Universal POS Support**: Works with PAX, Ingenico, Verifone, and other manufacturers
- 💳 **NFC Payments**: Tap-to-pay transactions with contactless cards/phones
- 🎵 **Audio Payments**: GGWave technology for sound-based data transmission
- 🔌 **Plugin Architecture**: Easy integration with manufacturer SDKs
- 🧪 **Mock Mode**: Test without real hardware
- 📱 **Android 6+**: Compatible with API level 23 and above

---

## What You'll Build

By following this guide, you'll create an Android app that can:

1. **Accept contactless payments** via NFC from customers' cards/phones
2. **Communicate via audio** using GGWave for contactless scenarios
3. **Process payment data** and integrate with payment gateways
4. **Work on any POS device** with proper plugin configuration

**Time to complete**: ~30 minutes for basic integration, ~2 hours for full setup

---

## Prerequisites

### 📋 Requirements
- **Android Studio** 4.0 or later
- **Android SDK** with minimum API level 23 (Android 6.0)
- **Java 17** (required for building)
- **Physical Android device** or **emulator with Google Play**
- **NFC-enabled device** (for NFC features)
- **Microphone & speakers** (for GGWave features)

### 🧠 Knowledge Assumed
- Basic Android development (Activities, Views)
- Understanding of Gradle dependencies
- Basic Java/Kotlin programming

### 🔧 Development Environment Setup
```bash
# Install Java 17 (macOS with Homebrew)
brew install openjdk@17

# Set JAVA_HOME
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home

# Verify installation
java -version
```

---

## Quick Start (5 Minutes)

### Step 1: Add Dependency
Add to your app's `build.gradle`:
```gradle
dependencies {
    // Option A: Local AAR (recommended for now)
    implementation files('libs/drishtipay-pos-sdk-0.0.1.aar')
    
    // Required dependencies
    implementation 'androidx.annotation:annotation:1.7.1'
    implementation 'androidx.webkit:webkit:1.8.0'
}
```

### Step 2: Add Permissions
Add to your `AndroidManifest.xml`:
```xml
<!-- NFC Permissions -->
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />

<!-- Audio Permissions (for GGWave) -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-feature android:name="android.hardware.microphone" android:required="false" />
```

### Step 3: Basic Integration
```java
public class MainActivity extends AppCompatActivity {
    private INfcDeviceManager nfcManager;
    private IGGWave ggWave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeDrishtiPaySDK();
    }

    private void initializeDrishtiPaySDK() {
        // Initialize NFC
        nfcManager = new PosNfcDeviceManager(this);
        
        // Initialize GGWave
        ggWave = new GGWaveImpl(this, true); // true = auto volume adjustment
        ggWave.initialize(() -> {
            Log.d("POS", "DrishtiPay SDK Ready!");
        });
    }
}
```

**🎉 Congratulations!** You now have basic POS SDK integration. Continue reading for full functionality.

---

## Detailed Setup

### 🏗️ Project Structure
```
your-android-app/
├── app/
│   ├── libs/
│   │   └── drishtipay-pos-sdk-0.0.1.aar    # SDK library
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/your/package/
│   │       └── MainActivity.java
│   └── build.gradle
└── build.gradle
```

### 📱 Android Manifest Configuration
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- App Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    
    <!-- NFC Permissions -->
    <uses-permission android:name="android.permission.NFC" />
    <uses-feature 
        android:name="android.hardware.nfc"
        android:required="false" />
    
    <!-- Audio Permissions (GGWave) -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-feature 
        android:name="android.hardware.microphone"
        android:required="false" />
    
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>
</manifest>
```

### 🔧 Gradle Configuration
**Project-level `build.gradle`:**
```gradle
buildscript {
    dependencies {
        classpath 'com.android.tools.build:gradle:8.0.0'
    }
}
```

**App-level `build.gradle`:**
```gradle
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "your.package.name"
        minSdk 23        // Android 6.0+
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    // Core Android
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // DrishtiPay POS SDK
    implementation files('libs/drishtipay-pos-sdk-0.0.1.aar')
    
    // Required for SDK
    implementation 'androidx.annotation:annotation:1.7.1'
    implementation 'androidx.webkit:webkit:1.8.0'
    
    // Optional: HTTP client for payment gateway integration
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
```

---

## Core Features

### 🏪 1. NFC Payment Processing

**Purpose**: Accept contactless payments from cards and mobile wallets

```java
public class NFCPaymentManager {
    private INfcDeviceManager nfcManager;
    
    public void initializeNFC(Context context) {
        // Option 1: Mock mode (for testing)
        nfcManager = new PosNfcDeviceManager(context);
        
        // Option 2: Real POS device (production)
        // PaxNeptuneLitePlugin plugin = new PaxNeptuneLitePlugin();
        // nfcManager = new PosNfcDeviceManager(context, plugin);
    }
    
    public void startListening() {
        nfcManager.startListening(new INfcDeviceManager.NdefCallback() {
            @Override
            public void onNdefMessageDiscovered(NdefMessage message) {
                // Customer tapped their card/phone
                processPayment(message);
            }
            
            @Override
            public void onError(String error) {
                Log.e("NFC", "Error: " + error);
            }
        });
    }
    
    private void processPayment(NdefMessage message) {
        // Extract payment data
        String paymentData = extractPaymentData(message);
        
        // Send to payment gateway
        // Your payment processing logic here
    }
}
```

### 🎵 2. GGWave Audio Communication

**Purpose**: Send/receive data via sound waves for contactless scenarios

```java
public class GGWaveManager {
    private IGGWave ggWave;
    
    public void initializeGGWave(Context context) {
        ggWave = new GGWaveImpl(context, true); // Auto volume adjustment
        
        ggWave.initialize(() -> {
            Log.d("GGWave", "Audio communication ready!");
        });
    }
    
    public void startListening() {
        ggWave.startListening(new IGGWave.GGWaveCallback() {
            @Override
            public boolean onMessageReceived(String message) {
                Log.d("GGWave", "Received: " + message);
                processAudioMessage(message);
                return true; // Continue listening
            }
            
            @Override
            public void onError(String error) {
                Log.e("GGWave", "Error: " + error);
            }
        });
    }
    
    public void sendMessage(String message) {
        ggWave.send(message, false, true, new IGGWave.GGWaveTransmissionCallback() {
            @Override
            public void onTransmissionComplete() {
                Log.d("GGWave", "Message sent successfully!");
            }
            
            @Override
            public void onTransmissionError(String error) {
                Log.e("GGWave", "Send failed: " + error);
            }
        });
    }
}
```

### 💳 3. Payment Gateway Integration

```java
public class PaymentProcessor implements IPayment {
    
    @Override
    public PaymentInitiationResponse initiatePayment(Card card, float amount) {
        try {
            // Create order with your payment gateway
            String orderId = createOrder(amount);
            
            // Process payment
            String response = processCardPayment(card, amount, orderId);
            
            return new PaymentInitiationResponse(response, "", orderId);
            
        } catch (Exception e) {
            Log.e("Payment", "Failed: " + e.getMessage());
            return null;
        }
    }
    
    private String createOrder(float amount) {
        // Your payment gateway API call
        // Example: Razorpay, Stripe, etc.
        return "order_" + System.currentTimeMillis();
    }
}
```

---

## Code Examples

### 🎯 Complete MainActivity Example

```java
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "POS_SDK";
    
    // SDK Components
    private INfcDeviceManager nfcManager;
    private IGGWave ggWave;
    private IPayment paymentProcessor;
    
    // UI Components
    private Button btnStartNFC, btnStopNFC;
    private Button btnStartGGWave, btnSendAudio;
    private TextView tvStatus, tvLastMessage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        initializeDrishtiPaySDK();
        setupButtonListeners();
    }
    
    private void initializeViews() {
        btnStartNFC = findViewById(R.id.btn_start_nfc);
        btnStopNFC = findViewById(R.id.btn_stop_nfc);
        btnStartGGWave = findViewById(R.id.btn_start_ggwave);
        btnSendAudio = findViewById(R.id.btn_send_audio);
        tvStatus = findViewById(R.id.tv_status);
        tvLastMessage = findViewById(R.id.tv_last_message);
    }
    
    private void initializeDrishtiPaySDK() {
        updateStatus("Initializing DrishtiPay SDK...");
        
        try {
            // Initialize NFC Manager
            nfcManager = new PosNfcDeviceManager(this);
            
            // Initialize GGWave
            ggWave = new GGWaveImpl(this, true);
            ggWave.initialize(() -> runOnUiThread(() -> {
                updateStatus("✅ DrishtiPay SDK Ready!");
                enableButtons(true);
            }));
            
            // Initialize Payment Processor
            paymentProcessor = new PaymentProcessor();
            
        } catch (Exception e) {
            updateStatus("❌ SDK Initialization Failed: " + e.getMessage());
            Log.e(TAG, "SDK init failed", e);
        }
    }
    
    private void setupButtonListeners() {
        btnStartNFC.setOnClickListener(v -> startNFCListening());
        btnStopNFC.setOnClickListener(v -> stopNFCListening());
        btnStartGGWave.setOnClickListener(v -> startGGWaveListening());
        btnSendAudio.setOnClickListener(v -> sendAudioMessage());
    }
    
    // NFC Methods
    private void startNFCListening() {
        updateStatus("🎧 NFC Listening...");
        nfcManager.startListening(new INfcDeviceManager.NdefCallback() {
            @Override
            public void onNdefMessageDiscovered(NdefMessage message) {
                runOnUiThread(() -> {
                    updateStatus("💳 Card detected! Processing...");
                    processNFCPayment(message);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> updateStatus("❌ NFC Error: " + error));
            }
        });
    }
    
    private void stopNFCListening() {
        nfcManager.stopListening();
        updateStatus("🔇 NFC Stopped");
    }
    
    private void processNFCPayment(NdefMessage message) {
        // Extract payment data from NFC message
        String paymentData = extractNFCData(message);
        updateLastMessage("NFC Payment: " + paymentData);
        
        // Process with payment gateway
        // Card mockCard = createMockCard(paymentData);
        // PaymentInitiationResponse response = paymentProcessor.initiatePayment(mockCard, 100.0f);
        
        updateStatus("✅ Payment Processed!");
    }
    
    // GGWave Methods
    private void startGGWaveListening() {
        updateStatus("🎤 Audio Listening...");
        ggWave.startListening(new IGGWave.GGWaveCallback() {
            @Override
            public boolean onMessageReceived(String message) {
                runOnUiThread(() -> {
                    updateStatus("📨 Audio message received!");
                    updateLastMessage("Audio: " + message);
                });
                return true;
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> updateStatus("❌ Audio Error: " + error));
            }
        });
    }
    
    private void sendAudioMessage() {
        String message = "POS-Payment-" + System.currentTimeMillis();
        updateStatus("📤 Sending audio message...");
        
        ggWave.send(message, false, true, new IGGWave.GGWaveTransmissionCallback() {
            @Override
            public void onTransmissionComplete() {
                runOnUiThread(() -> updateStatus("✅ Audio message sent!"));
            }
            
            @Override
            public void onTransmissionError(String error) {
                runOnUiThread(() -> updateStatus("❌ Send failed: " + error));
            }
        });
    }
    
    // Utility Methods
    private void updateStatus(String status) {
        tvStatus.setText(status);
        Log.d(TAG, status);
    }
    
    private void updateLastMessage(String message) {
        tvLastMessage.setText(message);
    }
    
    private void enableButtons(boolean enabled) {
        btnStartNFC.setEnabled(enabled);
        btnStartGGWave.setEnabled(enabled);
        btnSendAudio.setEnabled(enabled);
    }
    
    private String extractNFCData(NdefMessage message) {
        if (message.getRecords().length > 0) {
            return new String(message.getRecords()[0].getPayload());
        }
        return "Unknown payment data";
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup SDK resources
        if (nfcManager != null) {
            nfcManager.stopListening();
        }
        if (ggWave != null) {
            ggWave.cleanup();
        }
    }
}
```

---

## Testing

### 🧪 Unit Testing

```java
@RunWith(AndroidJUnit4.class)
public class PosSDKTest {
    
    private INfcDeviceManager nfcManager;
    private IGGWave ggWave;
    
    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        nfcManager = new PosNfcDeviceManager(context);
        ggWave = new GGWaveImpl(context, true);
    }
    
    @Test
    public void testNFCInitialization() {
        assertNotNull("NFC manager should be initialized", nfcManager);
    }
    
    @Test
    public void testGGWaveInitialization() {
        CountDownLatch latch = new CountDownLatch(1);
        
        ggWave.initialize(() -> {
            latch.countDown();
        });
        
        try {
            assertTrue("GGWave should initialize within 5 seconds", 
                      latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
    }
}
```

---

## Troubleshooting

### ❌ Common Issues & Solutions

#### 1. **"Unable to locate a Java Runtime"**
**Solution:**
```bash
# Install Java 17
brew install openjdk@17

# Set JAVA_HOME
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home
```

#### 2. **"NFC not working in emulator"**
**Solution:**
- Use physical device for NFC testing
- Or test with mock mode: `new PosNfcDeviceManager(context)` (no plugin)

#### 3. **"GGWave permission denied"**
**Solution:**
```bash
# Grant audio permission manually
adb shell pm grant your.package.name android.permission.RECORD_AUDIO
```

#### 4. **"Audio transmission not working"**
**Solutions:**
- Ensure devices have speakers/microphone
- Test in quiet environment
- Increase device volume
- Hold devices closer together (~1-2 feet)

---

## API Reference

### 📚 Core Interfaces

#### `INfcDeviceManager`
```java
public interface INfcDeviceManager {
    void startListening(NdefCallback callback);
    void stopListening();
    boolean isListening();
    
    interface NdefCallback {
        void onNdefMessageDiscovered(NdefMessage message);
        void onError(String errorMessage);
    }
}
```

#### `IGGWave`
```java
public interface IGGWave {
    void initialize(Runnable readyCallback);
    boolean send(String message, boolean useUltrasound, boolean fastMode, 
                 GGWaveTransmissionCallback callback);
    boolean send(String message);
    boolean startListening(GGWaveCallback callback);
    void stopListening();
    void cleanup();
}
```

---

## Best Practices

### ✅ Do's

1. **Always clean up resources** in `onDestroy()`
2. **Handle permissions gracefully** with user-friendly messages
3. **Use mock mode** for development and testing
4. **Test on real devices** before production
5. **Keep sensitive data secure** (never log payment details)

### ❌ Don'ts

1. **Don't hardcode payment credentials** in your app
2. **Don't ignore SDK initialization errors**
3. **Don't block the main thread** during payment processing
4. **Don't store sensitive payment data** locally without encryption

---

**🎉 You're Ready!** You now have everything needed to integrate DrishtiPay POS SDK into your Android application. Start with the Quick Start section and gradually add more features as needed.

**Good luck building your POS application!** 🚀