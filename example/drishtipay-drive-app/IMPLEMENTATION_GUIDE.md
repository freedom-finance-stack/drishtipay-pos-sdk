# DrishtiPay POS SDK Implementation Guide

## Overview

This project demonstrates how to implement the DrishtiPay POS SDK interfaces in your Android application. The SDK uses a plugin architecture that allows organizations to integrate their specific POS hardware while maintaining a unified API.

## Architecture

```
Your App
    ↓
DrishtiPay Core SDK (interfaces)
    ↓
Your Hardware Plugin (implementation)
    ↓
Manufacturer SDK (PAX Neptune Lite, Ingenico, etc.)
```

## Features Demonstrated

### 1. **Mock Mode for Development**
- ✅ Test NFC flows on emulator without real hardware
- ✅ Automatic NFC tap simulation
- ✅ Manual trigger for testing
- ✅ Comprehensive logging

### 2. **Real Hardware Integration**
- ✅ PAX Neptune Lite plugin architecture
- ✅ Placeholder for actual PAX SDK integration
- ✅ Switch between mock and real modes
- ✅ Error handling and lifecycle management

### 3. **Production-Ready UI**
- ✅ Material Design 3 interface
- ✅ Real-time status monitoring
- ✅ Payment processing simulation
- ✅ Activity logs with timestamps

## Quick Start

### 1. Add DrishtiPay SDK Dependency

```gradle
// In your app's build.gradle
dependencies {
    implementation files('libs/drishtipay-pos-sdk-0.0.1.aar')
}
```

### 2. Basic Implementation

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var nfcManager: INfcDeviceManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Option 1: Mock mode (for testing)
        nfcManager = PosNfcDeviceManager(this)
        
        // Option 2: With your hardware plugin
        // val yourPlugin = YourPaxPlugin() // Your implementation
        // nfcManager = PosNfcDeviceManager(this, yourPlugin)
        
        startPaymentListening()
    }
    
    private fun startPaymentListening() {
        nfcManager.startListening(object : INfcDeviceManager.NdefCallback {
            override fun onNdefMessageDiscovered(message: NdefMessage) {
                // Process payment data
                processPayment(message)
            }
            
            override fun onError(errorMessage: String) {
                // Handle errors
                Log.e("Payment", "NFC Error: $errorMessage")
            }
        })
    }
}
```

## Creating Your Hardware Plugin

### 1. Implement IPosNfcPlugin Interface

```java
public class YourPaxPlugin implements IPosNfcPlugin {
    
    @Override
    public void initialize(Context context) throws Exception {
        // Initialize your manufacturer SDK here
        // Example: NeptuneLiteUser.getInstance().getDal()
    }
    
    @Override
    public void startListening(INfcDeviceManager.NdefCallback callback) throws Exception {
        // Start NFC listening with your SDK
        // Example: nfcReader.detect(new INfcDetectCallback() { ... })
    }
    
    @Override
    public void stopListening() throws Exception {
        // Stop NFC and cleanup
    }
    
    // Implement other required methods...
}
```

### 2. Integration Examples

#### PAX Neptune Lite Integration
```java
// In your plugin's startListening method:
INFC nfcReader = NeptuneLiteUser.getInstance().getDal().getNfc();
nfcReader.detect(new INfcDetectCallback() {
    @Override
    public void onDetect(NfcDetectResult result) {
        // Convert PAX result to NDEF message
        NdefMessage message = convertPaxToNdef(result);
        callback.onNdefMessageDiscovered(message);
    }
});
```

#### Ingenico Integration
```java
// Example for Ingenico SDK
IngenicoNfcManager ingenicoNfc = new IngenicoNfcManager();
ingenicoNfc.startDetection(new IngenicoCallback() {
    @Override
    public void onCardDetected(CardData data) {
        // Convert Ingenico data to NDEF
        NdefMessage message = convertIngenicoToNdef(data);
        callback.onNdefMessageDiscovered(message);
    }
});
```

## Testing on Emulator

### 1. Enable Mock Mode
```kotlin
val paxPlugin = PaxNeptuneLitePlugin().apply {
    setMockMode(true)  // Enable mock mode
    setAutoSimulation(true, 3000)  // Auto-simulate after 3 seconds
}
val nfcManager = PosNfcDeviceManager(this, paxPlugin)
```

### 2. Manual Testing
```kotlin
// Trigger manual NFC simulation
if (paxPlugin.useMockMode) {
    paxPlugin.triggerTestNfcTap()
}
```

### 3. Custom Mock Data
```kotlin
// In your plugin, customize mock payment data:
private fun createMockPaymentNdefMessage(): NdefMessage {
    val mockData = "{"
        + "\"amount\": \"1250\","
        + "\"currency\": \"INR\","
        + "\"merchant_id\": \"your_merchant_id\","
        + "\"transaction_id\": \"txn_${System.currentTimeMillis()}\""
        + "}"
    
    val record = NdefRecord.createTextRecord("en", mockData)
    return NdefMessage(record)
}
```

## Production Deployment

### 1. Switch to Real Mode
```kotlin
// Disable mock mode for production
paxPlugin.setMockMode(false)
```

### 2. Error Handling
```kotlin
nfcManager.startListening(object : INfcDeviceManager.NdefCallback {
    override fun onError(errorMessage: String) {
        when {
            errorMessage.contains("not initialized") -> {
                // Reinitialize SDK
                reinitializeSDK()
            }
            errorMessage.contains("hardware") -> {
                // Show hardware error to user
                showHardwareError()
            }
            else -> {
                // Generic error handling
                showGenericError(errorMessage)
            }
        }
    }
})
```

### 3. Lifecycle Management
```kotlin
override fun onPause() {
    super.onPause()
    nfcManager.stopListening()  // Always stop when app goes background
}

override fun onDestroy() {
    super.onDestroy()
    if (nfcManager is PosNfcDeviceManager) {
        (nfcManager as PosNfcDeviceManager).cleanup()
    }
}
```

## Payment Processing Integration

### 1. Extract Payment Data
```kotlin
private fun processPayment(message: NdefMessage) {
    try {
        // Parse NDEF message
        val paymentData = extractPaymentData(message)
        val amount = paymentData.getString("amount")
        val currency = paymentData.getString("currency")
        
        // Send to payment gateway (Razorpay, etc.)
        processWithPaymentGateway(amount, currency)
        
    } catch (e: Exception) {
        handlePaymentError(e)
    }
}
```

### 2. Razorpay Integration Example
```kotlin
private fun processWithPaymentGateway(amount: String, currency: String) {
    // Example Razorpay integration
    val options = JSONObject().apply {
        put("name", "Your Merchant Name")
        put("currency", currency)
        put("amount", amount)
        put("order_id", generateOrderId())
    }
    
    // Process with Razorpay or your preferred gateway
    razorpay.open(this, options)
}
```

## Security Considerations

### 1. Data Validation
```kotlin
private fun validatePaymentData(data: String): Boolean {
    // Validate format, amount limits, etc.
    if (data.length > MAX_PAYLOAD_SIZE) return false
    if (!isValidJson(data)) return false
    // Add your validation logic
    return true
}
```

### 2. PII Protection
```kotlin
// Never log sensitive data
Log.d("Payment", "Processing payment for amount: ${amount}") // ✅ OK
Log.d("Payment", "Card data: ${cardDetails}") // ❌ Never do this
```

## Troubleshooting

### Common Issues

1. **SDK Not Initializing**
   ```
   Solution: Check if plugin.initialize() is called before startListening()
   ```

2. **NFC Not Working on Real Device**
   ```
   Solution: Ensure device has NFC enabled and your plugin correctly integrates manufacturer SDK
   ```

3. **Mock Mode Not Simulating**
   ```
   Solution: Check if setAutoSimulation(true, delay) is called and listening is active
   ```

### Debug Information
```kotlin
// Get detailed plugin information
Log.d("Debug", "Plugin Info: ${(nfcManager as PosNfcDeviceManager).pluginInfo}")
Log.d("Debug", "Supported Devices: ${(nfcManager as PosNfcDeviceManager).supportedDevices}")
Log.d("Debug", "Is Listening: ${(nfcManager as PosNfcDeviceManager).isListening}")
```

## Hardware Compatibility

### Supported POS Devices
- **PAX**: A920, A930, A35, A80, A77 (Neptune Lite compatible)
- **Ingenico**: iCT220, iCT250, Move/5000 series
- **Verifone**: VX520, VX820, VX680 series
- **Custom**: Any device with plugin implementation

### Requirements
- Android API Level 23+ (Android 6.0)
- NFC capability
- Manufacturer SDK (for real hardware)
- Hardware-specific permissions

## Next Steps

1. **Implement your hardware plugin** using the `IPosNfcPlugin` interface
2. **Test thoroughly** using mock mode on emulator
3. **Integrate payment gateway** for complete payment flow
4. **Deploy to real hardware** and test with actual POS devices
5. **Monitor and log** for production debugging

## Support

For additional support with DrishtiPay SDK integration:
- Check the core SDK documentation in `md_files/HOW_TO_USE.md`
- Review the example implementation in `SimplePosExample.java`
- Test with the demo app in this project

---

**Note**: This guide demonstrates the plugin architecture without exposing proprietary manufacturer SDK details. Organizations can implement their specific hardware integrations while keeping their licensed SDKs private.
