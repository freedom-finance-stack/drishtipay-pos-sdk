# How to Use Drishti Pay POS SDK

## 📦 Installation

### Step 1: Add to your app's `build.gradle`
```gradle
dependencies {
    implementation 'com.freedomfinancestack:pos-sdk-core:1.0.0'
}
```

### Step 2: Add to your project's `build.gradle`
```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        // When you publish to Maven Central, it will be available here
    }
}
```

## 🚀 Quick Start (3 Lines of Code!)

```java
// Step 1: Create NFC manager
INfcDeviceManager nfc = new PaxNfcDeviceManager(context);

// Step 2: Start listening
nfc.startListening(new INfcDeviceManager.NdefCallback() {
    @Override
    public void onNdefMessageDiscovered(NdefMessage message) {
        // Customer tapped phone - process payment!
    }
    
    @Override
    public void onError(String error) {
        // Handle error
    }
});

// Step 3: Stop when done
nfc.stopListening();
```

## 🏪 Real Example

```java
public class MyPosActivity extends Activity {
    
    private INfcDeviceManager nfcManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize for PAX machines
        nfcManager = new PaxNfcDeviceManager(this);
        
        // Start payment
        startPayment(100.00); // $100 payment
    }
    
    private void startPayment(double amount) {
        nfcManager.startListening(new INfcDeviceManager.NdefCallback() {
            @Override
            public void onNdefMessageDiscovered(NdefMessage message) {
                // Process payment with your gateway
                processPayment(message, amount);
            }
            
            @Override
            public void onError(String error) {
                showError("Payment failed: " + error);
            }
        });
    }
}
```

## 📱 Repository Publishing (For You)

To publish to Maven Central:

1. **Setup** your `build.gradle`:
```gradle
publishing {
    publications {
        maven(MavenPublication) {
            groupId = 'com.freedomfinancestack'
            artifactId = 'pos-sdk-core'
            version = '1.0.0'
            
            from components.java
        }
    }
}
```

2. **Publish**:
```bash
./gradlew publishToMavenCentral
```

3. **Others can then use**:
```gradle
implementation 'com.freedomfinancestack:pos-sdk-core:1.0.0'
```

## 🔊 GGWave Sound-Based Data Transmission

The SDK now includes GGWave functionality for sound-based data transmission between devices.

### Quick Start with Sound Data Transmission

```java
// Step 1: Create sound data transmission instance
ISoundDataTransmission soundTransmission = new SoundDataTransmissionImpl(context);

// Step 2: Send data via sound
soundTransmission.sendData("Hello World!", new ISoundDataTransmission.SoundTransmissionCallback() {
    @Override
    public void onDataSent(@NonNull String data) {
        Log.d(TAG, "Data sent successfully: " + data);
    }
    
    @Override
    public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
        Log.e(TAG, "Send failed: " + errorMessage);
    }
    
    // Other callback methods...
});

// Step 3: Listen for incoming data
soundTransmission.startListening(new ISoundDataTransmission.SoundTransmissionCallback() {
    @Override
    public void onDataReceived(@NonNull String data) {
        Log.d(TAG, "Received: " + data);
        // Process received data
    }
    
    @Override
    public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
        Log.e(TAG, "Receive failed: " + errorMessage);
    }
    
    // Other callback methods...
});

// Step 4: Cleanup when done
soundTransmission.cleanup();
```

### High-Level Workflow API

For more complex scenarios like device pairing and data transfer workflows:

```java
// Create device workflow manager
ISoundBasedDeviceWorkflow deviceWorkflow = new SoundBasedDeviceWorkflowImpl(context);

// Initiate pairing with another device
deviceWorkflow.initiatePairing("POS_TERMINAL_001", new ISoundBasedDeviceWorkflow.DevicePairingCallback() {
    @Override
    public void onPairingSuccess(@NonNull String deviceId) {
        Log.d(TAG, "Paired with: " + deviceId);
        
        // Transfer payment data to paired device
        String paymentData = "{\"amount\":\"25.99\",\"currency\":\"USD\"}";
        deviceWorkflow.transferData(paymentData, new ISoundBasedDeviceWorkflow.DataTransferCallback() {
            @Override
            public void onTransferSuccess(@NonNull String data) {
                Log.d(TAG, "Payment data sent successfully");
            }
            
            @Override
            public void onTransferFailed(@NonNull String errorMessage) {
                Log.e(TAG, "Transfer failed: " + errorMessage);
            }
            
            // Other callback methods...
        });
    }
    
    @Override
    public void onPairingFailed(@NonNull String errorMessage) {
        Log.e(TAG, "Pairing failed: " + errorMessage);
    }
    
    // Other callback methods...
});
```

### POS Payment Scenarios

#### Scenario 1: Send Payment Request to Customer

```java
public class PaymentActivity extends Activity {
    private ISoundDataTransmission soundTransmission;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        soundTransmission = new SoundDataTransmissionImpl(this);
        
        // Send payment request to customer's device
        sendPaymentRequest("25.99", "USD", "Coffee Shop");
    }
    
    private void sendPaymentRequest(String amount, String currency, String merchant) {
        String paymentRequest = String.format(
            "{\"type\":\"payment_request\",\"amount\":\"%s\",\"currency\":\"%s\",\"merchant\":\"%s\"}",
            amount, currency, merchant
        );
        
        soundTransmission.sendData(paymentRequest, new ISoundDataTransmission.SoundTransmissionCallback() {
            @Override
            public void onDataSent(@NonNull String data) {
                showMessage("Payment request sent to customer");
                // Start listening for payment confirmation
                listenForPaymentConfirmation();
            }
            
            @Override
            public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
                showMessage("Failed to send payment request: " + errorMessage);
            }
            
            @Override
            public void onTransmissionStarted() {
                showMessage("Sending payment request via sound...");
            }
            
            @Override
            public void onTransmissionCompleted() {
                Log.d(TAG, "Payment request transmission completed");
            }
            
            // Other methods...
        });
    }
    
    private void listenForPaymentConfirmation() {
        soundTransmission.startListening(new ISoundDataTransmission.SoundTransmissionCallback() {
            @Override
            public void onDataReceived(@NonNull String data) {
                if (data.contains("payment_confirmation")) {
                    showMessage("Payment confirmed! Transaction completed.");
                    soundTransmission.stopListening();
                }
            }
            
            @Override
            public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
                showMessage("Error receiving confirmation: " + errorMessage);
            }
            
            // Other methods...
        });
    }
}
```

#### Scenario 2: Device-to-Device Data Sync

```java
// Sync transaction data between POS terminals
ISoundBasedDeviceWorkflow deviceWorkflow = new SoundBasedDeviceWorkflowImpl(context);

// Terminal A: Wait for pairing
deviceWorkflow.waitForPairing(30000, new ISoundBasedDeviceWorkflow.DevicePairingCallback() {
    @Override
    public void onPairingSuccess(@NonNull String deviceId) {
        // Send daily sales data
        String salesData = getDailySalesData();
        deviceWorkflow.transferData(salesData);
    }
    
    // Other callback methods...
});

// Terminal B: Initiate pairing
deviceWorkflow.initiatePairing("TERMINAL_B", new ISoundBasedDeviceWorkflow.DevicePairingCallback() {
    @Override
    public void onPairingSuccess(@NonNull String deviceId) {
        // Request sales data from paired terminal
        deviceWorkflow.requestData("daily_sales", new ISoundBasedDeviceWorkflow.DataTransferCallback() {
            @Override
            public void onDataReceived(@NonNull String data) {
                processSalesData(data);
            }
            
            // Other callback methods...
        });
    }
    
    // Other callback methods...
});
```

### Required Permissions

Add to your `AndroidManifest.xml`:

```xml
<!-- Required for GGWave audio recording -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Optional: for better audio quality -->
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

### GGWave Configuration

```java
ISoundDataTransmission soundTransmission = new SoundDataTransmissionImpl(context);

// Set transmission volume (0.0 to 1.0)
soundTransmission.setTransmissionVolume(0.8f);

// Check if properly initialized
if (soundTransmission.isInitialized()) {
    // Ready to use
}

// Check if currently listening
if (soundTransmission.isListening()) {
    // Currently receiving data
}
```

### Best Practices

1. **Always cleanup resources:**
   ```java
   @Override
   protected void onDestroy() {
       super.onDestroy();
       if (soundTransmission != null) {
           soundTransmission.cleanup();
       }
   }
   ```

2. **Handle permissions properly:**
   ```java
   if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
       != PackageManager.PERMISSION_GRANTED) {
       ActivityCompat.requestPermissions(this, 
           new String[]{Manifest.permission.RECORD_AUDIO}, 
           REQUEST_AUDIO_PERMISSION);
   }
   ```

3. **Use appropriate volume levels:**
   ```java
   // For quiet environments
   soundTransmission.setTransmissionVolume(0.3f);
   
   // For noisy environments  
   soundTransmission.setTransmissionVolume(0.8f);
   ```

4. **Handle threading properly:**
   ```java
   // All sound transmission callbacks run on background threads
   // Update UI on main thread
   @Override
   public void onDataReceived(@NonNull String data) {
       runOnUiThread(() -> {
           updateUI(data);
       });
   }
   ```

## ✅ That's It!

Your SDK now includes:
- ✅ NFC functionality for contactless payments
- ✅ Sound-based data transmission (GGWave technology)
- ✅ High-level device workflow APIs
- ✅ Comprehensive examples and documentation
- ✅ Easy integration and deployment 