#include <jni.h>
#include <string>
#include <android/log.h>
#include <ggwave/ggwave.h>

#define LOG_TAG "GGWave-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

/**
 * Initialize GGWave instance
 */
JNIEXPORT jlong JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_initializeNative(
        JNIEnv *env, jobject thiz, jint sampleRate, jint samplesPerFrame) {
    
    try {
        ggwave::Parameters params;
        params.sampleRateInp = sampleRate;
        params.sampleRateOut = sampleRate;
        params.samplesPerFrame = samplesPerFrame;
        params.sampleFormatInp = GGWAVE_SAMPLE_FORMAT_F32;
        params.sampleFormatOut = GGWAVE_SAMPLE_FORMAT_F32;
        
        auto* instance = new ggwave::GGWave(params);
        
        LOGI("GGWave instance created successfully with sampleRate=%d, samplesPerFrame=%d", 
             sampleRate, samplesPerFrame);
        
        return reinterpret_cast<jlong>(instance);
    } catch (const std::exception& e) {
        LOGE("Failed to create GGWave instance: %s", e.what());
        return 0;
    }
}

/**
 * Start listening for incoming transmissions
 */
JNIEXPORT void JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_startListeningNative(
        JNIEnv *env, jobject thiz, jlong instancePtr) {
    
    if (instancePtr == 0) {
        LOGE("Invalid GGWave instance pointer");
        return;
    }
    
    auto* instance = reinterpret_cast<ggwave::GGWave*>(instancePtr);
    
    try {
        // GGWave doesn't have explicit start/stop listening - it processes audio frames
        LOGI("GGWave listening started");
    } catch (const std::exception& e) {
        LOGE("Error starting GGWave listening: %s", e.what());
    }
}

/**
 * Stop listening for transmissions
 */
JNIEXPORT void JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_stopListeningNative(
        JNIEnv *env, jobject thiz, jlong instancePtr) {
    
    if (instancePtr == 0) {
        LOGE("Invalid GGWave instance pointer");
        return;
    }
    
    try {
        LOGI("GGWave listening stopped");
    } catch (const std::exception& e) {
        LOGE("Error stopping GGWave listening: %s", e.what());
    }
}

/**
 * Capture and decode audio data
 */
JNIEXPORT jstring JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_captureAndDecodeNative(
        JNIEnv *env, jobject thiz, jlong instancePtr) {
    
    if (instancePtr == 0) {
        LOGE("Invalid GGWave instance pointer");
        return nullptr;
    }
    
    auto* instance = reinterpret_cast<ggwave::GGWave*>(instancePtr);
    
    try {
        // Note: In a real implementation, you would capture audio from microphone
        // and pass the audio frames to GGWave for decoding
        // For now, return null as no data is captured
        
        // Example of how to process audio frames:
        // std::vector<float> audioFrame(samplesPerFrame);
        // // Fill audioFrame with captured audio data
        // ggwave::TxRxData result = instance->decode(audioFrame);
        // if (!result.empty()) {
        //     std::string decoded(result.begin(), result.end());
        //     return env->NewStringUTF(decoded.c_str());
        // }
        
        return nullptr;
    } catch (const std::exception& e) {
        LOGE("Error capturing/decoding audio: %s", e.what());
        return nullptr;
    }
}

/**
 * Encode data to audio
 */
JNIEXPORT jbyteArray JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_encodeToAudioNative(
        JNIEnv *env, jobject thiz, jlong instancePtr, jstring data) {
    
    if (instancePtr == 0) {
        LOGE("Invalid GGWave instance pointer");
        return nullptr;
    }
    
    if (data == nullptr) {
        LOGE("Data string is null");
        return nullptr;
    }
    
    auto* instance = reinterpret_cast<ggwave::GGWave*>(instancePtr);
    
    try {
        // Convert Java string to C++ string
        const char* dataStr = env->GetStringUTFChars(data, nullptr);
        std::string dataToEncode(dataStr);
        env->ReleaseStringUTFChars(data, dataStr);
        
        LOGI("Encoding data: %s", dataToEncode.substr(0, 20).c_str());
        
        // Encode the data using default protocol (can be parameterized later)
        ggwave::TxRxData txData(dataToEncode.begin(), dataToEncode.end());
        auto waveform = instance->encode(txData);
        
        if (waveform.empty()) {
            LOGE("Failed to encode data to waveform");
            return nullptr;
        }
        
        // Convert float waveform to byte array for Android AudioTrack
        size_t numSamples = waveform.size();
        size_t bytesSize = numSamples * sizeof(float);
        
        jbyteArray result = env->NewByteArray(bytesSize);
        if (result == nullptr) {
            LOGE("Failed to allocate byte array");
            return nullptr;
        }
        
        env->SetByteArrayRegion(result, 0, bytesSize, 
                               reinterpret_cast<const jbyte*>(waveform.data()));
        
        LOGI("Successfully encoded data to %zu samples", numSamples);
        return result;
        
    } catch (const std::exception& e) {
        LOGE("Error encoding data to audio: %s", e.what());
        return nullptr;
    }
}

/**
 * Encode data with specific protocol
 */
JNIEXPORT jbyteArray JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_encodeToAudioWithProtocolNative(
        JNIEnv *env, jobject thiz, jlong instancePtr, jstring data, jint protocolId) {
    
    if (instancePtr == 0) {
        LOGE("Invalid GGWave instance pointer");
        return nullptr;
    }
    
    if (data == nullptr) {
        LOGE("Data string is null");
        return nullptr;
    }
    
    auto* instance = reinterpret_cast<ggwave::GGWave*>(instancePtr);
    
    try {
        // Convert Java string to C++ string
        const char* dataStr = env->GetStringUTFChars(data, nullptr);
        std::string dataToEncode(dataStr);
        env->ReleaseStringUTFChars(data, dataStr);
        
        LOGI("Encoding data with protocol %d: %s", protocolId, dataToEncode.substr(0, 20).c_str());
        
        // Map protocol ID to GGWave protocol
        ggwave::ProtocolId ggwaveProtocol;
        switch (protocolId) {
            case 0: ggwaveProtocol = GGWAVE_PROTOCOL_AUDIBLE_NORMAL; break;
            case 1: ggwaveProtocol = GGWAVE_PROTOCOL_AUDIBLE_FAST; break;
            case 2: ggwaveProtocol = GGWAVE_PROTOCOL_AUDIBLE_FASTEST; break;
            case 3: ggwaveProtocol = GGWAVE_PROTOCOL_ULTRASOUND_NORMAL; break;
            case 4: ggwaveProtocol = GGWAVE_PROTOCOL_ULTRASOUND_FAST; break;
            case 5: ggwaveProtocol = GGWAVE_PROTOCOL_ULTRASOUND_FASTEST; break;
            case 6: ggwaveProtocol = GGWAVE_PROTOCOL_DT_NORMAL; break;
            case 7: ggwaveProtocol = GGWAVE_PROTOCOL_DT_FAST; break;
            case 8: ggwaveProtocol = GGWAVE_PROTOCOL_DT_FASTEST; break;
            default:
                LOGE("Unknown protocol ID: %d", protocolId);
                return nullptr;
        }
        
        // Encode the data using specified protocol
        ggwave::TxRxData txData(dataToEncode.begin(), dataToEncode.end());
        auto waveform = instance->encode(txData, ggwaveProtocol, 10); // 10 volume level
        
        if (waveform.empty()) {
            LOGE("Failed to encode data to waveform with protocol %d", protocolId);
            return nullptr;
        }
        
        // Convert float waveform to byte array
        size_t numSamples = waveform.size();
        size_t bytesSize = numSamples * sizeof(float);
        
        jbyteArray result = env->NewByteArray(bytesSize);
        if (result == nullptr) {
            LOGE("Failed to allocate byte array");
            return nullptr;
        }
        
        env->SetByteArrayRegion(result, 0, bytesSize, 
                               reinterpret_cast<const jbyte*>(waveform.data()));
        
        LOGI("Successfully encoded data to %zu samples with protocol %d", numSamples, protocolId);
        return result;
        
    } catch (const std::exception& e) {
        LOGE("Error encoding data to audio with protocol: %s", e.what());
        return nullptr;
    }
}

/**
 * Cleanup GGWave instance
 */
JNIEXPORT void JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_cleanupNative(
        JNIEnv *env, jobject thiz, jlong instancePtr) {
    
    if (instancePtr == 0) {
        LOGE("Invalid GGWave instance pointer");
        return;
    }
    
    try {
        auto* instance = reinterpret_cast<ggwave::GGWave*>(instancePtr);
        delete instance;
        LOGI("GGWave instance cleaned up successfully");
    } catch (const std::exception& e) {
        LOGE("Error cleaning up GGWave instance: %s", e.what());
    }
}

} // extern "C"