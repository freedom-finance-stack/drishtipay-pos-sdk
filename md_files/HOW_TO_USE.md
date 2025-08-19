# POS SDK Core - How to Use

## 📱 Overview

This SDK provides **minimalistic APIs** for POS (Point of Sale) system integration, focusing on:
- **NFC device management** for card readers and payment terminals
- **Sound-based data transmission** for device communication
- **Simple, production-ready interfaces** with minimal learning curve

## 🚀 Quick Start

### Dependencies

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.freedomfinancestack:pos-sdk-core:1.0.0'
}
```

### Permissions

Add to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
```

## 💳 NFC Device Management

### Basic NFC Setup

```java
// Initialize NFC manager with your plugin
IPosNfcPlugin yourPlugin = new YourManufacturerPlugin();
INfcDeviceManager nfcManager = new PosNfcDeviceManager(yourPlugin);

// Start listening for NFC events
nfcManager.startListening(new NdefCallback() {
    @Override
    public void onNdefMessageDiscovered(NdefMessage message) {
        // Process NFC payment card data
        processPaymentCard(message);
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "NFC Error: " + error);
    }
});

// Stop when done
nfcManager.stopListening();
```

### Plugin Implementation

Create your manufacturer-specific plugin:

```java
public class YourPosPlugin implements IPosNfcPlugin {
    
    @Override
    public void initialize(Context context) throws Exception {
        // Initialize your manufacturer's SDK here
    }
    
    @Override
    public void startListening(NdefCallback callback) throws Exception {
        // Start your device's NFC listening
    }
    
    @Override
    public void stopListening() {
        // Stop NFC operations
    }
    
    // Other required methods...
}
```

## 🔊 Sound Data Transmission

### Minimalistic Sound API

The SDK provides **ultra-simple sound transmission** - just **listen**, **send**, and **stop**:

```java
// 1. Create instance
ISoundDataTransmission sound = new SoundDataTransmissionImpl(context);

// 2. Listen for data
sound.listen(new ISoundDataTransmission.SoundCallback() {
    @Override
    public void onReceived(@NonNull String data) {
        Log.d(TAG, "Received: " + data);
        // Process payment data
    }
    
    @Override
    public void onSent(@NonNull String data) {
        // Not used when listening
    }
    
    @Override
    public void onError(@NonNull String error) {
        Log.e(TAG, "Error: " + error);
    }
});

// 3. Send data (with callback)
sound.send("Payment confirmed", new ISoundDataTransmission.SoundCallback() {
    @Override
    public void onReceived(@NonNull String data) {
        // Not used when sending
    }
    
    @Override
    public void onSent(@NonNull String data) {
        Log.d(TAG, "Sent: " + data);
    }
    
    @Override
    public void onError(@NonNull String error) {
        Log.e(TAG, "Send failed: " + error);
    }
});

// OR send without callback (fire-and-forget)
sound.send("Quick message");

// 4. Stop when done
sound.stop();
```

### Sound Transmission Features

- **🔇 Silent Operation**: Uses inaudible ultrasound frequencies
- **⚡ Fast Transmission**: Optimized for quick POS transactions
- **🎯 Simple API**: Just 3 methods - `listen()`, `send()`, `stop()`
- **🧵 Background Threading**: Non-blocking operations
- **📱 Native Performance**: Real GGWave C++ library integration

### Sound Data Limits

```java
// Maximum data length
int maxLength = 140; // characters

// Validate before sending
if (data.length() <= maxLength) {
    sound.send(data);
} else {
    Log.e(TAG, "Data too long");
}
```

## 📚 Complete Example

```java
public class PosActivity extends AppCompatActivity {
    
    private INfcDeviceManager nfcManager;
    private ISoundDataTransmission sound;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize NFC
        IPosNfcPlugin plugin = new YourManufacturerPlugin();
        nfcManager = new PosNfcDeviceManager(plugin);
        
        // Initialize sound transmission
        sound = new SoundDataTransmissionImpl(this);
        
        startPosOperations();
    }
    
    private void startPosOperations() {
        // Start NFC listening
        nfcManager.startListening(new NdefCallback() {
            @Override
            public void onNdefMessageDiscovered(NdefMessage message) {
                processPayment(message);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "NFC Error: " + error);
            }
        });
        
        // Start sound listening
        sound.listen(new ISoundDataTransmission.SoundCallback() {
            @Override
            public void onReceived(@NonNull String data) {
                handleReceivedData(data);
            }
            
            @Override
            public void onSent(@NonNull String data) {
                // Data sent successfully
            }
            
            @Override
            public void onError(@NonNull String error) {
                Log.e(TAG, "Sound Error: " + error);
            }
        });
    }
    
    private void processPayment(NdefMessage message) {
        // Process NFC payment card
        // ...
        
        // Send confirmation via sound
        sound.send("Payment processed - Receipt #12345");
    }
    
    private void handleReceivedData(String data) {
        // Handle received sound data
        Log.d(TAG, "Sound data: " + data);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup resources
        if (nfcManager != null) {
            nfcManager.stopListening();
        }
        
        if (sound != null) {
            sound.stop();
        }
    }
}
```

## 🛠️ Advanced Configuration

### Build Configuration

The SDK includes native library support. Ensure your `build.gradle` has:

```gradle
android {
    compileSdk 34
    
    defaultConfig {
        minSdk 23
        targetSdk 34
        
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'
        }
    }
    
    buildFeatures {
        prefab true
    }
}
```

### ProGuard Configuration

The SDK includes ProGuard rules automatically. No additional configuration needed.

## 🔧 Troubleshooting

### Common Issues

1. **NFC not working**: Ensure device has NFC capability and permissions are granted
2. **Sound transmission fails**: Check RECORD_AUDIO permission and device microphone/speaker
3. **Native library errors**: Verify NDK configuration and supported architectures

### Debugging

Enable verbose logging:

```java
// The SDK automatically logs important events
// Check logcat with tag filters:
// - "SoundDataTransmission"
// - "PosNfcDeviceManager"
```

### Error Handling

All SDK operations use callbacks for error reporting:

```java
// Always implement error callbacks
@Override
public void onError(@NonNull String error) {
    Log.e(TAG, "SDK Error: " + error);
    // Handle error appropriately
    // - Show user message
    // - Retry operation
    // - Fall back to alternative method
}
```

## 📝 Best Practices

1. **Always cleanup resources** in `onDestroy()`
2. **Handle errors gracefully** with appropriate user feedback
3. **Test on real devices** - emulators may not support all features
4. **Check permissions** before starting operations
5. **Use fire-and-forget sending** for non-critical data
6. **Implement timeouts** for critical operations

## 🏗️ Architecture

The SDK follows clean, minimalistic architecture:

```
┌─────────────────┐    ┌─────────────────┐
│   Your App      │    │   POS SDK Core  │
├─────────────────┤    ├─────────────────┤
│ Activity/       │───▶│ INfcDeviceManager│
│ Fragment        │    │ ISoundDataTrans │
│                 │    │ (3 methods only)│
└─────────────────┘    └─────────────────┘
                              │
                       ┌─────────────────┐
                       │ Native Libraries│
                       ├─────────────────┤
                       │ Your NFC Plugin │
                       │ GGWave Library  │
                       └─────────────────┘
```

**Key Principles:**
- ✅ **Minimal API Surface** - Only essential methods exposed
- ✅ **Plugin Architecture** - Manufacturer SDKs via plugins
- ✅ **Native Performance** - JNI integration for sound transmission
- ✅ **Clean Separation** - Core SDK independent of specific hardware

## 📞 Support

For issues and questions:
- Check the troubleshooting section above
- Review the example code in the repository
- Ensure all permissions and dependencies are correctly configured