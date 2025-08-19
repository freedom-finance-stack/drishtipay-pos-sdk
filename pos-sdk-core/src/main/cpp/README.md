# GGWave Native Library Integration

This directory contains the native C++ integration for GGWave sound-based data transmission in the POS SDK.

## 📁 Structure

```
cpp/
├── CMakeLists.txt          # CMake build configuration
├── ggwave_jni.cpp          # JNI wrapper for GGWave
├── ggwave/                 # GGWave library (git submodule)
└── README.md               # This file
```

## 🛠️ Build Requirements

- **Android NDK**: Required for native library compilation
- **CMake**: Version 3.22.1 or higher
- **C++17**: Standard required by GGWave library

## 🔧 Integration Details

### GGWave Library
- **Source**: https://github.com/ggerganov/ggwave
- **Integration**: Git submodule at `ggwave/`
- **License**: MIT

### JNI Wrapper (`ggwave_jni.cpp`)
Provides Java-to-C++ bridge for:
- GGWave instance initialization
- Audio encoding with protocol support
- Audio decoding and listening
- Resource cleanup

### Protocol Mapping
Java enum `GGWaveProtocol` maps to native GGWave protocol IDs:

| Java Protocol ID | GGWave Protocol | Frequency Range |
|------------------|-----------------|-----------------|
| 0 | `GGWAVE_PROTOCOL_AUDIBLE_NORMAL` | 1875-6375 Hz |
| 1 | `GGWAVE_PROTOCOL_AUDIBLE_FAST` | 1875-6375 Hz |
| 2 | `GGWAVE_PROTOCOL_AUDIBLE_FASTEST` | 1875-6375 Hz |
| 3 | `GGWAVE_PROTOCOL_ULTRASOUND_NORMAL` | 15000-19500 Hz |
| 4 | `GGWAVE_PROTOCOL_ULTRASOUND_FAST` | 15000-19500 Hz |
| 5 | `GGWAVE_PROTOCOL_ULTRASOUND_FASTEST` ⭐ | 15000-19500 Hz |
| 6 | `GGWAVE_PROTOCOL_DT_NORMAL` | 1125-2625 Hz |
| 7 | `GGWAVE_PROTOCOL_DT_FAST` | 1125-2625 Hz |
| 8 | `GGWAVE_PROTOCOL_DT_FASTEST` | 1125-2625 Hz |

⭐ Default protocol for POS transactions

## 🏗️ Build Process

1. **CMake Configuration**: Automatically handled by Android Gradle Plugin
2. **Native Compilation**: Compiles GGWave library and JNI wrapper
3. **Library Packaging**: Creates `libpos_sdk_ggwave.so` for each architecture
4. **Java Loading**: Library loaded via `System.loadLibrary("pos_sdk_ggwave")`

## 🎯 Supported Architectures

- `arm64-v8a` (64-bit ARM)
- `armeabi-v7a` (32-bit ARM)
- `x86` (32-bit Intel)
- `x86_64` (64-bit Intel)

## 🔍 Debugging

### Build Issues
```bash
# Clean and rebuild
./gradlew :pos-sdk-core:clean
./gradlew :pos-sdk-core:assembleDebug

# Check native build logs
./gradlew :pos-sdk-core:assembleDebug --info
```

### Runtime Issues
- Check native library loading in logcat: `GGWave native library loaded successfully`
- Verify NDK is properly installed
- Ensure all required architectures are built

## 📚 References

- [GGWave Documentation](https://github.com/ggerganov/ggwave)
- [Android NDK Guide](https://developer.android.com/ndk/guides)
- [CMake Android Documentation](https://developer.android.com/ndk/guides/cmake)