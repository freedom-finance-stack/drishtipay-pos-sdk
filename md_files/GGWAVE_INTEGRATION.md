# GGWave Integration Guide

## Overview

The DrishtiPay POS SDK now includes **GGWave** functionality, enabling **audio-based data transmission** for contactless communication. This allows POS devices to send and receive data using sound waves, providing an alternative to NFC for scenarios where physical proximity is required but NFC is not available.

## Features

- **Audio Data Transmission**: Send text data over sound waves
- **Audio Data Reception**: Receive and decode audio messages in real-time
- **Multiple Protocols**: Support for audible and ultrasound frequencies
- **Speed Options**: Fast mode for quick transmission, normal mode for reliability
- **Volume Management**: Automatic volume adjustment during transmission
- **Thread-Safe**: All callbacks executed on main thread
- **Resource Management**: Proper cleanup and lifecycle management

## Prerequisites

### Permissions

Add these permissions to your app's `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.INTERNET" />

<uses-feature
    android:name="android.hardware.microphone"
    android:required="false" />
```

### Runtime Permission

Request `RECORD_AUDIO` permission at runtime:

```java
if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
}
```

## Quick Start

### 1. Initialize GGWave

```java
import com.freedomfinancestack.pos_sdk_core.implementations.GGWaveImpl;
import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWave;

public class MyActivity extends Activity {
    private IGGWave ggWave;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create GGWave instance
        ggWave = new GGWaveImpl(this, true); // true = auto volume adjustment
        
        // Initialize
        ggWave.initialize(() -> {
            Log.d("GGWave", "Ready to use!");
            // Start using GGWave functionality
        });
    }
}
```

### 2. Send Data

```java
// Simple send
ggWave.send("Hello World");

// Advanced send with options
ggWave.send(
    "Payment data: {amount: 100, currency: 'USD'}", 
    true,  // useUltrasound - for privacy
    false, // fastMode - for reliability 
    new IGGWave.GGWaveTransmissionCallback() {
        @Override
        public void onTransmissionComplete() {
            Log.d("GGWave", "Message sent successfully");
        }
        
        @Override
        public void onTransmissionError(String error) {
            Log.e("GGWave", "Send failed: " + error);
        }
    }
);
```

### 3. Receive Data

```java
ggWave.startListening(new IGGWave.GGWaveCallback() {
    @Override
    public boolean onMessageReceived(String message) {
        Log.d("GGWave", "Received: " + message);
        
        // Process the message
        processMessage(message);
        
        // Return true to continue listening, false to stop
        return true;
    }
    
    @Override
    public void onError(String error) {
        Log.e("GGWave", "Reception error: " + error);
    }
});
```

### 4. Cleanup

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (ggWave != null) {
        ggWave.cleanup();
    }
}
```

## Advanced Usage

### Payment Data Transmission

```java
// Send payment request via ultrasound for privacy
public void sendPaymentRequest(String paymentJson) {
    ggWave.send(
        paymentJson,
        true,  // ultrasound mode for privacy
        false, // normal mode for reliability
        new IGGWave.GGWaveTransmissionCallback() {
            @Override
            public void onTransmissionComplete() {
                updateUI("Payment request sent");
            }
            
            @Override
            public void onTransmissionError(String error) {
                showError("Failed to send payment: " + error);
            }
        }
    );
}

// Listen for payment responses
public void listenForPaymentResponse() {
    ggWave.startListening(new IGGWave.GGWaveCallback() {
        @Override
        public boolean onMessageReceived(String message) {
            if (message.contains("PAYMENT_CONFIRMED")) {
                handlePaymentConfirmation(message);
                return false; // Stop listening after confirmation
            }
            return true; // Continue listening
        }
        
        @Override
        public void onError(String error) {
            handlePaymentError(error);
        }
    });
}
```

### Configuration Options

```java
// Create with custom settings
IGGWave ggWave = new GGWaveImpl(context, autoAdjustVolume);

// Check status
if (ggWave.isInitialized()) {
    // Ready to use
}

if (ggWave.isListening()) {
    // Currently listening for messages
}

// Stop listening
ggWave.stopListening();
```

## Audio Protocols

### Frequency Options

1. **Audible Range** (`useUltrasound = false`)
   - Frequencies that humans can hear
   - Better for debugging and testing
   - Longer range transmission

2. **Ultrasound Range** (`useUltrasound = true`)
   - Near-ultrasound frequencies (less audible)
   - Better for privacy
   - Shorter range but more discrete

### Speed Options

1. **Fast Mode** (`fastMode = true`)
   - Quicker transmission
   - Higher risk of errors
   - Good for short messages

2. **Normal Mode** (`fastMode = false`)
   - Slower but more reliable
   - Better error correction
   - Recommended for important data

## Best Practices

### 1. Permission Handling
```java
private void checkAndRequestPermissions() {
    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
    } else {
        initializeGGWave();
    }
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == REQUEST_AUDIO && grantResults.length > 0 && 
        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        initializeGGWave();
    } else {
        showError("Audio permission required for GGWave functionality");
    }
}
```

### 2. Error Handling
```java
private void initializeWithErrorHandling() {
    try {
        ggWave = new GGWaveImpl(this, true);
        ggWave.initialize(() -> {
            Log.d("GGWave", "Initialization successful");
        });
    } catch (IllegalStateException e) {
        Log.e("GGWave", "Initialization failed: " + e.getMessage());
        showError("Please grant audio permission to use this feature");
    }
}
```

### 3. Lifecycle Management
```java
@Override
protected void onPause() {
    super.onPause();
    if (ggWave != null) {
        ggWave.stopListening(); // Stop audio processing when app is in background
    }
}

@Override
protected void onResume() {
    super.onResume();
    if (ggWave != null && ggWave.isInitialized()) {
        // Restart listening if needed
        startListeningIfNeeded();
    }
}
```

### 4. Message Format
```java
// Use structured data for reliable parsing
public void sendStructuredData() {
    JSONObject data = new JSONObject();
    data.put("type", "payment_request");
    data.put("amount", 100.00);
    data.put("currency", "USD");
    data.put("merchantId", "MERCHANT_123");
    
    ggWave.send(data.toString(), true, false, callback);
}

// Parse received structured data
private void parseReceivedData(String message) {
    try {
        JSONObject data = new JSONObject(message);
        String type = data.getString("type");
        
        switch (type) {
            case "payment_request":
                handlePaymentRequest(data);
                break;
            case "payment_response":
                handlePaymentResponse(data);
                break;
            default:
                Log.w("GGWave", "Unknown message type: " + type);
        }
    } catch (JSONException e) {
        Log.e("GGWave", "Failed to parse message: " + e.getMessage());
    }
}
```

## Security Considerations

1. **Use Ultrasound for Sensitive Data**: Enable ultrasound mode for payment data
2. **Validate All Input**: Never trust received audio data without validation
3. **Implement Timeouts**: Don't listen indefinitely for responses
4. **Log Safely**: Never log sensitive payment information

```java
// Secure payment transmission
private void sendSecurePayment(PaymentData payment) {
    String encryptedData = encryptPaymentData(payment); // Your encryption
    
    ggWave.send(
        encryptedData,
        true,  // Use ultrasound for privacy
        false, // Use normal mode for reliability
        new TransmissionCallback()
    );
}
```

## Troubleshooting

### Common Issues

1. **"RECORD_AUDIO permission required"**
   - Solution: Request runtime permission before initializing

2. **"WebView initialization failed"**
   - Solution: Ensure app has INTERNET permission
   - Check if WebView is available on the device

3. **No audio received**
   - Check microphone permissions
   - Ensure devices are close enough (< 2 meters)
   - Test with audible mode first

4. **Transmission errors**
   - Use normal mode instead of fast mode
   - Check for background noise interference
   - Ensure volume is adequate

### Debug Tips

```java
// Enable verbose logging
private static final String TAG = "GGWave";

// Test with audible mode first
ggWave.send("test", false, true, new IGGWave.GGWaveTransmissionCallback() {
    @Override
    public void onTransmissionComplete() {
        Log.d(TAG, "Test transmission successful");
    }
    
    @Override
    public void onTransmissionError(String error) {
        Log.e(TAG, "Test failed: " + error);
    }
});
```

## Integration with Existing POS Features

The GGWave functionality seamlessly integrates with existing POS SDK features:

```java
public class CompletePOSExample extends Activity {
    private INfcDeviceManager nfcManager;
    private IGGWave ggWave;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize both NFC and Audio functionality
        setupNFC();
        setupGGWave();
    }
    
    private void processPayment(String paymentData, String source) {
        Log.d(TAG, "Processing payment from: " + source);
        
        // Same payment processing logic regardless of input method
        PaymentResult result = processPaymentData(paymentData);
        
        // Send confirmation via the same channel
        if ("NFC".equals(source)) {
            sendNFCResponse(result);
        } else if ("AUDIO".equals(source)) {
            sendAudioResponse(result);
        }
    }
}
```

## API Reference

See the complete API documentation in the [IGGWave interface](../pos-sdk-core/src/main/java/com/freedomfinancestack/pos_sdk_core/interfaces/IGGWave.java) and [GGWaveExample](../pos-sdk-core/src/main/java/com/freedomfinancestack/pos_sdk_core/examples/GGWaveExample.java).