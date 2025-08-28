# How to Import DrishtiPay POS SDK as Local Dependency

This document provides a complete step-by-step guide for importing the private DrishtiPay POS SDK as a local dependency in any Android project.

## Prerequisites

- Android Studio installed
- GitHub CLI (`gh`) installed and authenticated
- Access to the private repository `freedom-finance-stack/drishtipay-pos-sdk`

## Quick Note

📁 **Local Repository Available**: A local copy of the DrishtiPay POS SDK repository is already available at:
```
/Users/user/AndroidStudioProjects/drishtipay-pos-sdk
```
This guide will use this existing local copy for building the AAR.

## Step-by-Step Guide

### 1. Setup GitHub Authentication

**Option A: Using Environment Variables (Recommended for Security)**

Use the provided setup script:

```bash
# Make the script executable (if not already)
chmod +x setup_env.sh

# Run the interactive setup
./setup_env.sh
```

**Option B: Manual Environment Variable Setup**

```bash
# Set environment variables for current session
export GITHUB_USERNAME="your_github_username"
export GITHUB_ACCESS_TOKEN="your_github_personal_access_token"

# Or add to your shell profile for permanent use
echo 'export GITHUB_USERNAME="your_github_username"' >> ~/.zshrc
echo 'export GITHUB_ACCESS_TOKEN="your_github_personal_access_token"' >> ~/.zshrc
source ~/.zshrc
```

**Option C: GitHub CLI Authentication (Alternative)**

```bash
# Install GitHub CLI (if not installed)
# macOS: brew install gh
# Windows: Download from https://cli.github.com/

# Login to GitHub
gh auth login
```

Follow the prompts to authenticate with your GitHub account.

### 2. Clone the Private Repository

**Option A: Use Existing Local Copy (Recommended)**

If you already have a local copy of the repository:

```bash
# Navigate to the existing repository
cd /Users/user/AndroidStudioProjects/drishtipay-pos-sdk

# Ensure you're on master branch and pull latest changes
git checkout master
git pull origin master
```

**Option B: Clone Fresh Repository**

```bash
# Navigate to your Android Studio projects directory
cd /path/to/AndroidStudioProjects

# Clone the private repository
gh repo clone freedom-finance-stack/drishtipay-pos-sdk
cd drishtipay-pos-sdk
git checkout master
```

### 3. Configure Android SDK for the Repository

```bash
# If not already in the repository directory
cd /Users/user/AndroidStudioProjects/drishtipay-pos-sdk

# Copy local.properties from your main project (or create manually)
# Replace YOUR_PROJECT_NAME with your actual project name
cp ../YOUR_PROJECT_NAME/local.properties .

# OR create manually with this content:
echo "sdk.dir=/Users/$(whoami)/Library/Android/sdk" > local.properties
```

### 4. Build the SDK Library

```bash
# Ensure you're in the correct directory and on master branch
cd /Users/user/AndroidStudioProjects/drishtipay-pos-sdk
git checkout master

# Clean previous builds and build the release AAR file
./gradlew clean pos-sdk-core:assembleRelease

# Verify the AAR file was created
find . -name "*.aar" -type f
# Should show: ./pos-sdk-core/build/outputs/aar/pos-sdk-core-release.aar
```

### 5. Copy AAR to Your Project

```bash
# Navigate to your Android project
cd ../YOUR_PROJECT_NAME

# Create libs directory if it doesn't exist
mkdir -p app/libs

# Copy the AAR file with a versioned name from the existing local repository
cp /Users/user/AndroidStudioProjects/drishtipay-pos-sdk/pos-sdk-core/build/outputs/aar/pos-sdk-core-release.aar app/libs/drishtipay-pos-sdk-0.0.1.aar

# Verify the file was copied
ls -la app/libs/drishtipay-pos-sdk-0.0.1.aar
```

### 6. Update Your Project's build.gradle

Add the dependency to your `app/build.gradle` file:

```gradle
dependencies {
    // ... your existing dependencies ...
    
    // DrishtiPay POS SDK - Local AAR file
    implementation files('libs/drishtipay-pos-sdk-0.0.1.aar')
    
    // ... rest of your dependencies ...
}
```

### 7. Sync and Build Your Project

```bash
# Clean and build your project
./gradlew clean build

# Or just sync dependencies
./gradlew --refresh-dependencies
```

### 8. Basic Integration Example

Add this to your `MainActivity` or relevant Activity:

```kotlin
import com.freedomfinancestack.pos_sdk_core.implementations.PosNfcDeviceManager
import com.freedomfinancestack.pos_sdk_core.interfaces.INfcDeviceManager

class MainActivity : ComponentActivity() {
    
    private lateinit var nfcManager: INfcDeviceManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize DrishtiPay POS SDK
        initializeDrishtiPaySDK()
    }
    
    private fun initializeDrishtiPaySDK() {
        try {
            // Initialize the POS NFC Device Manager in mock mode
            nfcManager = PosNfcDeviceManager(this)
            Log.d("MainActivity", "DrishtiPay POS SDK initialized successfully!")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize DrishtiPay SDK", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up SDK resources
        if (::nfcManager.isInitialized) {
            nfcManager.stopListening()
            if (nfcManager is PosNfcDeviceManager) {
                (nfcManager as PosNfcDeviceManager).cleanup()
            }
        }
    }
}
```

## Alternative Methods (Backup Options)

### Option A: Using JitPack (if SDK gets published)

1. Add JitPack repository to `settings.gradle`:
```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

2. Add dependency in `app/build.gradle`:
```gradle
implementation 'com.github.freedom-finance-stack.drishtipay-pos-sdk:pos-sdk-core:v0.0.1'
```

### Option B: Using GitHub Packages (if SDK gets published)

1. Add GitHub Packages repository to `settings.gradle`:
```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/freedom-finance-stack/drishtipay-pos-sdk")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}
```

2. Set environment variables:
```bash
export GITHUB_USERNAME="your_github_username"
export GITHUB_ACCESS_TOKEN="your_github_personal_access_token"
```

Or add to your shell profile (~/.zshrc, ~/.bashrc):
```bash
echo 'export GITHUB_USERNAME="your_github_username"' >> ~/.zshrc
echo 'export GITHUB_ACCESS_TOKEN="your_github_personal_access_token"' >> ~/.zshrc
source ~/.zshrc
```

3. Add dependency in `app/build.gradle`:
```gradle
implementation 'com.freedomfinancestack:drishtipay-pos-sdk:0.0.1'
```

## Updating the SDK

When a new version of the SDK is released:

1. Pull latest changes in the existing local repository:
```bash
cd /Users/user/AndroidStudioProjects/drishtipay-pos-sdk
git checkout master
git pull origin master
```

2. Rebuild the AAR:
```bash
./gradlew clean pos-sdk-core:assembleRelease
```

3. Copy the new AAR to your project:
```bash
cp /Users/user/AndroidStudioProjects/drishtipay-pos-sdk/pos-sdk-core/build/outputs/aar/pos-sdk-core-release.aar ../YOUR_PROJECT_NAME/app/libs/drishtipay-pos-sdk-NEW_VERSION.aar
```

4. Update the dependency in `app/build.gradle`:
```gradle
implementation files('libs/drishtipay-pos-sdk-NEW_VERSION.aar')
```

## Troubleshooting

### Build Errors
- **SDK location not found**: Ensure `local.properties` exists with correct `sdk.dir` path
- **Permission denied**: Make sure you have access to the private repository
- **Dependency not found**: Verify the AAR file exists in `app/libs/` directory

### Authentication Issues
- **gh auth login fails**: Try `gh auth refresh` or re-authenticate
- **Repository not accessible**: Ensure you have read access to the private repository

### Integration Issues
- **Import errors**: Make sure the AAR is properly added to dependencies
- **Runtime crashes**: Check if all required permissions are added to `AndroidManifest.xml`

## Project Structure After Setup

```
your-android-project/
├── app/
│   ├── libs/
│   │   └── drishtipay-pos-sdk-0.0.1.aar  ← Local SDK dependency
│   ├── src/main/java/.../MainActivity.kt ← Integration example
│   └── build.gradle                      ← Dependency configuration
├── local.properties                      ← Android SDK path
└── settings.gradle                       ← Repository configuration
```

## Available SDK Classes

Key classes you can use after integration:

- `PosNfcDeviceManager` - Main NFC device manager
- `INfcDeviceManager` - Interface for NFC operations  
- `PaymentStatus` - Payment status enum
- `CardType` - Card type enum
- `Network` - Card network enum
- `Card` - Card model class
- `PaymentInitiationResponse` - Payment response model

## Next Steps

1. Explore the SDK documentation and examples
2. Implement payment flows using the provided interfaces
3. Test in mock mode before production deployment
4. Configure actual POS device plugins for production use

---

✅ **Integration Complete!** Your project now has the DrishtiPay POS SDK available as a local dependency.
