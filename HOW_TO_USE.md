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

## ✅ That's It!

Your SDK is now:
- ✅ Simple (3 lines to use)
- ✅ PAX-focused
- ✅ Easy to publish
- ✅ Easy for others to integrate 