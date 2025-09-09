# DrishtiPay POS SDK

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=23)
[![Java Version](https://img.shields.io/badge/Java-11-blue.svg)](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)

> **Merchant-side POS SDK enabling accessible payments for visually impaired customers**

## 🏪 About This SDK

The **DrishtiPay POS SDK** provides the technical foundation for implementing the [DrishtiPay open standard](https://github.com/freedom-finance-stack/drishtipay) on merchant Point of Sale systems. This comprehensive, plugin-based Android library enables POS manufacturers and payment processors to support **accessible payments for visually impaired customers**.

Built with a modular architecture, this SDK supports multiple POS devices through a clean plugin interface, ensuring universal compatibility while maintaining the highest security standards.

### 🔗 Part of the DrishtiPay Ecosystem

- 🏠 **[DrishtiPay](https://github.com/freedom-finance-stack/drishtipay)** - Complete ecosystem overview and documentation
- 📱 **[DrishtiPay Mobile SDK](https://github.com/freedom-finance-stack/drishtipay-mobile-sdk)** - Customer-side mobile implementation
- 🏪 **DrishtiPay POS SDK** - *You are here* - Merchant-side POS integration

## Demo Video


https://github.com/user-attachments/assets/3e53570a-c2d1-4cfe-88df-63b9f64f3c6e



## Example Implementation
https://github.com/freedom-finance-stack/drishtipay-pos-sdk/tree/master/example/drishtipay-drive-app


## 🚀 Features

### Core Capabilities
- **Universal NFC Support**: Unified interface for NFC payment processing across different POS devices
- **Plugin Architecture**: Extensible design supporting PAX, Ingenico, Verifone, and other POS manufacturers
- **Audio Data Transmission**: GGWave-powered sound-based data transfer for legacy POS compatibility
- **Accessibility Focused**: Designed to eliminate PIN sharing for visually impaired customers
- **Payment Processing**: Comprehensive payment handling with support for cards, UPI, and contactless payments
- **Mock & Real Modes**: Development-friendly with simulation capabilities for testing without hardware

### Accessibility Benefits
- **🔒 Enhanced Privacy**: Eliminates need for customers to share PINs verbally
- **🛡️ Improved Security**: Secure smartphone-based authentication
- **♿ Inclusive Design**: Supports assistive technologies and screen readers
- **🤝 Merchant Friendly**: Easy integration with existing POS infrastructure


## 📋 Table of Contents

- [Installation](#-installation)
- [Quick Start](#-quick-start)
- [Architecture](#-architecture)
- [API Reference](#-api-reference)
- [Examples](#-examples)
- [GGWave Integration](#-ggwave-integration)
- [Build Requirements](#-build-requirements)
- [License](#-license)
- [Acknowledgments](#-acknowledgments)
- [Support](#-support)

## 🛠 Installation

### Gradle Dependency

Add the following to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.freedomfinancestack:pos-sdk-core:1.0.0'
    
    // Required for GGWave functionality
    implementation 'androidx.webkit:webkit:1.8.0'
    implementation 'androidx.annotation:annotation:1.7.1'
}
```

### Permissions

Add these permissions to your `AndroidManifest.xml`:

```xml
<!-- NFC permissions -->
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />

<!-- Audio permissions for GGWave -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-feature android:name="android.hardware.microphone" android:required="false" />

<!-- Internet permission  -->
<uses-permission android:name="android.permission.INTERNET" />
```

## 🚀 Quick Start

### Basic NFC Payment Processing

```java
// Initialize the POS NFC Device Manager
INfcDeviceManager nfcManager = new PosNfcDeviceManager();

// Set up your plugin (example with PAX)
IPosNfcPlugin paxPlugin = new PaxNeptuneLitePlugin();
paxPlugin.initialize(context);

// Register the plugin
((PosNfcDeviceManager) nfcManager).setPlugin(paxPlugin);

// Start listening for NFC payments
nfcManager.startListening(new INfcDeviceManager.NdefCallback() {
    @Override
    public void onNdefMessageDiscovered(NdefMessage message) {
        // Process payment data
        Log.d("Payment", "Payment received: " + new String(message.getRecords()[0].getPayload()));
    }
    
    @Override
    public void onError(String errorMessage) {
        Log.e("Payment", "Payment error: " + errorMessage);
    }
});
```

### Audio Data Transmission with GGWave

```java
// Initialize GGWave
IGGWave ggWave = new GGWaveImpl(context, true); // auto-adjust volume

ggWave.initialize(() -> {
    // Send a message over audio
    ggWave.send("Hello POS World!", false, true, new IGGWave.GGWaveTransmissionCallback() {
        @Override
        public void onTransmissionComplete() {
            Log.d("GGWave", "Message sent successfully");
        }
        
        @Override
        public void onTransmissionError(String error) {
            Log.e("GGWave", "Transmission failed: " + error);
        }
    });
});

// Listen for incoming audio messages
ggWave.startListening(new IGGWave.GGWaveCallback() {
    @Override
    public boolean onMessageReceived(GGWaveMessage message) {
        Log.d("GGWave", "Received DrishtiPay message from: " + message.getMobileNumber());
        return true; // Continue listening
    }
    
    @Override
    public boolean onRawMessageReceived(String rawMessage) {
        Log.d("GGWave", "Received raw message: " + rawMessage);
        return true;
    }
    
    @Override
    public void onError(String error) {
        Log.e("GGWave", "Reception error: " + error);
    }
});
```

## 🏗 Architecture

The SDK follows a clean plugin architecture that separates core functionality from manufacturer-specific implementations:

```
┌─────────────────────────────────────────────────────────┐
│                    Your Application                     │
├─────────────────────────────────────────────────────────┤
│                    POS SDK Core                         │
│  ┌─────────────────┐  ┌─────────────────┐               │
│  │   NFC Manager   │  │   GGWave Audio  │               │
│  │   (Universal)   │  │   Transmission  │               │
│  └─────────────────┘  └─────────────────┘               │
├─────────────────────────────────-───────────────────────┤
│                  Plugin Interface                       │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │ PAX Plugin  │  │Ingenico     │  │  Your       │      │
│  │ (Neptune)   │  │Plugin       │  │  Custom     │      │
│  │             │  │             │  │  Plugin     │      │
│  └─────────────┘  └─────────────┘  └─────────────┘      │
└─────────────────────────────────────────────────────────┘
```

### Core Components

- **`pos-sdk-core`**: Core library with interfaces and reference implementations
- **`INfcDeviceManager`**: Universal NFC interface for all POS devices
- **`IPosNfcPlugin`**: Plugin interface for manufacturer-specific implementations
- **`IGGWave`**: Audio-based data transmission interface
- **`examples/`**: Sample implementations and integration examples

## 🔌 Plugin Development

### Creating a Custom POS Plugin

Implement the `IPosNfcPlugin` interface for your POS hardware:

```java
public class MyPosPlugin implements IPosNfcPlugin {
    
    @Override
    public void initialize(Context context) throws Exception {
        // Initialize your POS SDK
    }
    
    @Override
    public void startListening(INfcDeviceManager.NdefCallback callback) throws Exception {
        // Start NFC detection using your hardware SDK
    }
    
    @Override
    public void stopListening() throws Exception {
        // Stop NFC detection and cleanup
    }
    
    @Override
    public String getPluginInfo() {
        return "My POS Plugin v1.0 - Supports XYZ POS devices";
    }
    
    @Override
    public String getSupportedDevices() {
        return "Model A, Model B, Model C";
    }
    
    // ... implement other required methods
}
```

## 📚 API Reference

### Core Interfaces

#### `INfcDeviceManager`
- `startListening(NdefCallback callback)`: Start NFC listening
- `stopListening()`: Stop NFC listening

#### `IPosNfcPlugin`
- `initialize(Context context)`: Initialize plugin
- `startListening(NdefCallback callback)`: Start hardware-specific NFC detection
- `stopListening()`: Stop NFC detection
- `isListening()`: Check listening status
- `getPluginInfo()`: Get plugin information
- `getSupportedDevices()`: Get supported device list
- `cleanup()`: Clean up resources

#### `IGGWave`
- `initialize(Runnable readyCallback)`: Initialize GGWave
- `send(String message, ...)`: Send text over audio
- `sendMessage(GGWaveMessage message, ...)`: Send structured message
- `startListening(GGWaveCallback callback)`: Listen for audio messages
- `stopListening()`: Stop audio listening
- `cleanup()`: Clean up audio resources

### Models

#### `GGWaveMessage`
```java
// Create a DrishtiPay message
GGWaveMessage message = new GGWaveMessage("9876543210");

// Create custom message
GGWaveMessage custom = new GGWaveMessage("9876543210", "my_app", "custom_type");

// Parse from JSON
GGWaveMessage parsed = GGWaveMessage.fromJson(jsonString);
```

## 💡 Examples

The SDK includes comprehensive examples in the `examples/` directory:

### Available Examples

1. **`SimplePosExample.java`**: Basic NFC payment processing
2. **`GGWaveExample.java`**: Audio data transmission examples
3. **`PaxNeptuneLitePlugin.java`**: PAX POS device integration
4. **`AbstractPosPlugin.java`**: Universal demo plugin for presentations

### Running Examples

```bash
# Build the example app
cd example/drishtipay-drive-app
./gradlew assembleDebug

# Install and run
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🎵 GGWave Integration

This SDK integrates [GGWave](https://github.com/ggerganov/ggwave) for audio-based data transmission, enabling contactless data exchange through sound waves.

### GGWave Features

- **Ultrasound & Audible**: Support for both frequency ranges
- **Fast & Normal Modes**: Balance between speed and reliability
- **Cross-Platform**: Works with web, mobile, and desktop applications
- **No Internet Required**: Pure audio-based communication

### Attribution

The GGWave functionality in this SDK is based on the excellent work by [Georgi Gerganov](https://github.com/ggerganov/ggwave). We've integrated and adapted the GGWave library to work seamlessly with Android POS systems.

**Original GGWave Project**: https://github.com/ggerganov/ggwave

### GGWave Usage Examples

```java
// Send payment request over ultrasound (more private)
ggWave.send(paymentJson, true, false, callback);

// Send mobile number using DrishtiPay format
ggWave.sendMobileNumber("9876543210");

// Listen for structured DrishtiPay messages
ggWave.startListening(callback);
```


## 🔧 Build Requirements

- **Android Studio**: Arctic Fox or later
- **Java**: JDK 11 or later
- **Android SDK**: API 23+ (minSdk), API 36 (target/compile)
- **Gradle**: 7.0+

### Build Commands

```bash
# Build the core library
./gradlew :pos-sdk-core:assemble

# Build with all checks
./gradlew :pos-sdk-core:assemble :pos-sdk-core:lint 

# Generate AAR
./gradlew :pos-sdk-core:assembleRelease
```

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **[GGWave](https://github.com/ggerganov/ggwave)** by Georgi Gerganov - Audio data transmission library

## How to contribute to the project
For contribution guidelines, see [CONTRIBUTING](contributing.md).

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/freedom-finance-stack/drishtipay-pos-sdk/issues)
- **Accessibility Support**: [contact@freedomfinancestack.com](mailto:contact@freedomfinancestack.com)
- **Technical Support**: [contact@freedomfinancestack.com](mailto:contact@freedomfinancestack.com)

---

**Made with ❤️ by the Freedom Finance Stack Team**

*Building the future of financial technology, one API at a time.*
