# 🏪 DrishtiPay POS SDK

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?style=flat)](https://android.com)
[![Java 11](https://img.shields.io/badge/Java-11-orange.svg?style=flat)](https://openjdk.java.net/projects/jdk/11/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat)](LICENSE)
[![MinSDK](https://img.shields.io/badge/MinSDK-23-red.svg?style=flat)](https://developer.android.com/about/versions/marshmallow/android-6.0)



**Universal Android SDK for POS machine NFC payment processing** - supports PAX, Ingenico, Verifone, and any manufacturer through a plugin architecture.

---

## 🎯 Overview

DrishtiPay POS SDK is a production-ready Android library that enables seamless NFC payment processing across different Point of Sale (POS) manufacturers. Built with a **plugin architecture**, it provides a unified API while allowing organizations to integrate their specific manufacturer SDKs.

### ✨ Key Features

- 🔌 **Plugin Architecture** - Core defines interfaces, organizations bring manufacturer implementations
- 🏪 **Multi-Manufacturer Support** - PAX, Ingenico, Verifone, and custom implementations
- 📱 **NFC Payment Processing** - Contactless payments with phones and cards
- 💳 **Card Management** - Save, list, and manage customer payment cards
- 🔒 **Secure & PCI Compliant** - Built-in security best practices
- 🧪 **Mock Mode** - Test without manufacturer hardware
- 📚 **Simple Integration** - 3 lines of code to get started

### 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│              Your POS App               │
├─────────────────────────────────────────┤
│           DrishtiPay Core SDK           │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │ Interfaces  │  │ Implementations │   │
│  │             │  │                 │   │
│  │ INfcDevice  │  │ PosNfcDevice    │   │
│  │ Manager     │  │ Manager         │   │
│  │             │  │                 │   │
│  │ IPosNfc     │  │ Mock Plugin     │   │
│  │ Plugin      │  │ (Testing)       │   │
│  │             │  │                 │   │
│  │ IPayment    │  │                 │   │
│  │ ICards      │  │                 │   │
│  └─────────────┘  └─────────────────┘   │
├─────────────────────────────────────────┤
│        Manufacturer Plugins             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐    │
│  │   PAX   │ │Ingenico │ │Verifone │    │
│  │ Plugin  │ │ Plugin  │ │ Plugin  │    │
│  └─────────┘ └─────────┘ └─────────┘    │
└─────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Installation

**Step 1:** Add to your app's `build.gradle`:
```gradle
dependencies {
    implementation 'com.freedomfinancestack:pos-sdk-core:1.0.0'
}
```

**Step 2:** Add to your project's `build.gradle`:
```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

### Basic Usage (3 Lines!)

```java
// Step 1: Create NFC manager
INfcDeviceManager nfc = new PosNfcDeviceManager(context);

// Step 2: Start listening for payments
nfc.startListening(new INfcDeviceManager.NdefCallback() {
    @Override
    public void onNdefMessageDiscovered(NdefMessage message) {
        // Customer tapped phone - process payment!
        processPayment(message);
    }
    
    @Override
    public void onError(String error) {
        // Handle error
        Log.e("Payment", "Error: " + error);
    }
});

// Step 3: Stop when done
nfc.stopListening();
```

---

## 💼 Production Integration

### Plugin-Based Setup

For production with manufacturer SDKs:

```java
public class PaymentActivity extends Activity {
    
    private INfcDeviceManager nfcManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize with your manufacturer plugin
        IPosNfcPlugin plugin = new YourManufacturerPlugin();
        nfcManager = new PosNfcDeviceManager(this, plugin);
        
        startPaymentSession();
    }
    
    private void startPaymentSession() {
        nfcManager.startListening(new INfcDeviceManager.NdefCallback() {
            @Override
            public void onNdefMessageDiscovered(NdefMessage message) {
                // Process the payment
                handlePayment(message);
            }
            
            @Override
            public void onError(String error) {
                showError("Payment failed: " + error);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nfcManager != null) {
            nfcManager.stopListening();
            ((PosNfcDeviceManager) nfcManager).cleanup();
        }
    }
}
```

### Creating Manufacturer Plugins

Implement `IPosNfcPlugin` for your POS hardware:

```java
public class YourManufacturerPlugin implements IPosNfcPlugin {
    
    @Override
    public void initialize(Context context) throws Exception {
        // Initialize your manufacturer SDK
        YourManufacturerSDK.init(context);
    }
    
    @Override
    public void startListening(INfcDeviceManager.NdefCallback callback) throws Exception {
        // Start NFC listening using manufacturer SDK
        YourManufacturerSDK.startNfc(callback);
    }
    
    @Override
    public void stopListening() throws Exception {
        // Stop NFC listening
        YourManufacturerSDK.stopNfc();
    }
    
    @Override
    public boolean isListening() {
        return YourManufacturerSDK.isNfcActive();
    }
    
    @Override
    public String getPluginInfo() {
        return "YourManufacturer Plugin v1.0";
    }
    
    @Override
    public String getSupportedDevices() {
        return "PAX A920, A930, Neptune Lite";
    }
    
    @Override
    public void cleanup() {
        YourManufacturerSDK.cleanup();
    }
}
```

---

## 📱 Payment & Card Management

### Payment Processing

```java
// Initialize payment interface
IPayment paymentService = new YourPaymentImplementation();

// Create card object
Card card = Card.builder()
    .cardId("card_123")
    .last4Digits("1234")
    .network(Network.VISA)
    .cardType(CardType.CREDIT)
    .issuerBank(IssuerBank.HDFC)
    .build();

// Initiate payment
PaymentInitiationResponse response = paymentService.initiatePayment(card, 100.0f);

// Handle 3D Secure if required
if (response.getAcsURL() != null) {
    // Redirect to ACS URL for OTP verification
    redirectTo3DSecure(response.getAcsURL());
}

// Confirm payment status
PaymentStatus status = paymentService.confirmPayment(response.getPaymentId());
```

### Card Management

```java
// Initialize cards interface
ICards cardsService = new YourCardsImplementation();

// List saved cards for a merchant and contact
List<ListSavedCards> savedCards = cardsService.listAllSavedCards(
    "merchant_123", 
    "customer_contact"
);

// Process saved cards
for (ListSavedCards cardList : savedCards) {
    Card[] cards = cardList.getCards();
    for (Card card : cards) {
        System.out.println("Card: " + card.getNetwork() + " ending in " + card.getLast4Digits());
    }
}
```

---

## 🏗️ Core Components

### Interfaces

| Interface | Purpose | Methods |
|-----------|---------|---------|
| `INfcDeviceManager` | NFC device operations | `startListening()`, `stopListening()` |
| `IPosNfcPlugin` | Manufacturer plugin contract | `initialize()`, `startListening()`, `stopListening()`, `cleanup()` |
| `IPayment` | Payment processing | `initiatePayment()`, `confirmPayment()` |
| `ICards` | Card management | `listAllSavedCards()` |

### Models

| Model | Purpose | Key Fields |
|-------|---------|------------|
| `Card` | Payment card data | `cardId`, `last4Digits`, `network`, `cardType`, `issuerBank` |
| `PaymentInitiationResponse` | Payment response | `acsURL`, `paymentId`, `orderId` |
| `CardToken` | Card tokenization | `id`, `merchantId`, `createdAt` |
| `ListSavedCards` | Saved cards container | `contact`, `cards[]` |

### Enums

| Enum | Values |
|------|--------|
| `Network` | `MASTERCARD`, `VISA`, `RUPAY`, `AMEX`, `DINERS` |
| `CardType` | `CREDIT`, `DEBIT` |
| `PaymentStatus` | `PAID`, `CREATED` |
| `IssuerBank` | `HDFC`, `AXIS`, `ICICI`, `SBI` |

---

## 🧪 Testing & Development

### Mock Mode

Test without manufacturer hardware:

```java
// Use mock mode for development
INfcDeviceManager nfcManager = new PosNfcDeviceManager(context);

// Mock plugin simulates NFC taps after 3 seconds
nfcManager.startListening(callback);
```

### Build Commands

```bash
# Build the library
./gradlew :pos-sdk-core:assemble

# Run tests
./gradlew :pos-sdk-core:test

# Run lint checks
./gradlew :pos-sdk-core:lint

# Build all
./gradlew clean build
```

---

## 🔒 Security & Compliance

### Data Protection
- ✅ **No PII Logging** - Card numbers and sensitive data are never logged
- ✅ **PCI DSS Compliant** - Follows payment card industry standards
- ✅ **Secure Tokenization** - Card data is tokenized, not stored
- ✅ **Input Validation** - All NFC payloads are validated and sanitized

### Best Practices
- ✅ **Thread Safety** - All callbacks are thread-safe
- ✅ **Resource Management** - Automatic cleanup of NFC resources
- ✅ **Error Handling** - Comprehensive error handling with actionable messages
- ✅ **ProGuard Ready** - Obfuscation rules included

---

## 📋 Requirements

| Requirement | Version/Details |
|------------|-----------------|
| **Platform** | Android API 23+ (Android 6.0) |
| **Java** | Java 11+ |
| **Build Tool** | Gradle 8.11.1+ |
| **Hardware** | NFC-enabled POS device |
| **Permissions** | `android.permission.NFC` |

---

## 🏭 Supported Manufacturers

| Manufacturer | Models | Plugin Required |
|--------------|--------|-----------------|
| **PAX** | A920, A930, Neptune Lite | ✅ |
| **Ingenico** | iCT220, iCT250, Move/5000 | ✅ |
| **Verifone** | VX820, VX690, VX675 | ✅ |
| **Custom** | Any manufacturer | ✅ |
| **Mock** | Testing/Development | ❌ (Built-in) |

---

## 📚 Documentation

- 📖 **[How to Use Guide](md_files/HOW_TO_USE.md)** - Detailed usage instructions
- 🔧 **[API Reference](docs/api/)** - Complete API documentation
- 🏗️ **[Plugin Development](docs/plugins/)** - Creating manufacturer plugins
- 🧪 **[Testing Guide](docs/testing/)** - Testing strategies and examples

---

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/freedom-finance-stack/drishtipay-pos-sdk.git
   cd drishtipay-pos-sdk
   ```

2. **Build the project**
   ```bash
   ./gradlew clean build
   ```

3. **Run tests**
   ```bash
   ./gradlew test
   ```

### Code Quality Standards

- ✅ **Java 11** compatibility
- ✅ **Android Lint** compliance
- ✅ **Unit tests** for core functionality
- ✅ **Javadoc** for public APIs
- ✅ **ProGuard** rules for consumer apps

---

## 📦 Publishing

To publish to Maven Central:

```gradle
// build.gradle
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

```bash
./gradlew publishToMavenCentral
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🆘 Support

- 📧 **Email**: support@freedomfinancestack.com
- 🐛 **Issues**: [GitHub Issues](https://github.com/freedom-finance-stack/drishtipay-pos-sdk/issues)
- 📖 **Documentation**: [Wiki](https://github.com/freedom-finance-stack/drishtipay-pos-sdk/wiki)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/freedom-finance-stack/drishtipay-pos-sdk/discussions)

---

## 🚀 Roadmap

- [ ] **Bluetooth Support** - Bluetooth payment device integration
- [ ] **QR Code Payments** - UPI QR code support
- [ ] **EMV Chip Support** - Chip card processing
- [ ] **Receipt Generation** - Digital and print receipt support
- [ ] **Multi-Currency** - International payment support
- [ ] **Analytics Dashboard** - Payment analytics and reporting

---

**Built with ❤️ by the Freedom Finance Stack team**

*Making POS payments simple, secure, and universal.*
