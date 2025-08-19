#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstring>

#include "ggwave/include/ggwave/ggwave.h"

#define LOG_TAG "GGWave-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

/**
 * Initialize GGWave instance using C API (like official ggwave-java)
 */
JNIEXPORT jlong JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_initializeNative(
        JNIEnv *env, jobject thiz, jint sampleRate, jint samplesPerFrame) {
    
    try {
        // Use C API like official ggwave-java implementation
        ggwave_Parameters parameters = ggwave_getDefaultParameters();
        parameters.sampleFormatInp = GGWAVE_SAMPLE_FORMAT_I16;
        parameters.sampleFormatOut = GGWAVE_SAMPLE_FORMAT_I16;
        parameters.sampleRateInp = sampleRate;
        parameters.sampleRateOut = sampleRate;
        parameters.samplesPerFrame = samplesPerFrame;
        
        ggwave_Instance instance = ggwave_init(parameters);
        
        LOGI("ggwave_init returned instance ID: %d", instance);
        
        if (instance < 0) {
            LOGE("Failed to initialize ggwave instance (returned %d)", instance);
            return 0;
        }
        
        LOGI("GGWave instance created successfully with sampleRate=%d, samplesPerFrame=%d, instance=%d", 
             sampleRate, samplesPerFrame, instance);
        
        return static_cast<jlong>(instance);
        
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
    
    if (instancePtr < 0) {
        LOGE("Invalid GGWave instance pointer: %ld", instancePtr);
        return;
    }
    
    LOGI("Starting to listen for transmissions");
    // Note: In C API, listening is handled by continuously calling decode on captured audio
}

/**
 * Stop listening for transmissions
 */
JNIEXPORT void JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_stopListeningNative(
        JNIEnv *env, jobject thiz, jlong instancePtr) {
    
    if (instancePtr < 0) {
        LOGE("Invalid GGWave instance pointer: %ld", instancePtr);
        return;
    }
    
    LOGI("Stopping transmission listening");
    // Note: In C API, stopping is handled by stopping the audio capture in Java
}

/**
 * Decode captured audio data using C API
 * Note: This is a simplified version - in a full implementation, you would
 * need to integrate with Android's AudioRecord API to capture real audio data
 */
JNIEXPORT jstring JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_captureAndDecodeNative(
        JNIEnv *env, jobject thiz, jlong instancePtr) {
    
    if (instancePtr < 0) {
        LOGE("Invalid GGWave instance pointer: %ld", instancePtr);
        return nullptr;
    }
    
    // Note: In a real implementation, this would capture audio from microphone
    // and call ggwave_decode with the captured data. For now, we return null
    // to indicate no data received.
    
    return nullptr;
}

/**
 * Encode data to audio using C API (like official ggwave-java)
 */
JNIEXPORT jbyteArray JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_encodeToAudioWithProtocolNative(
        JNIEnv *env, jobject thiz, jlong instancePtr, jstring data, jint protocolId) {
    
    if (instancePtr < 0) {
        LOGE("Invalid GGWave instance pointer: %ld", instancePtr);
        return nullptr;
    }
    
    ggwave_Instance instance = static_cast<ggwave_Instance>(instancePtr);
    
    const char* dataToEncode = env->GetStringUTFChars(data, nullptr);
    int dataLength = env->GetStringLength(data);
    
    if (dataToEncode == nullptr) {
        LOGE("Failed to get string data");
        return nullptr;
    }
    
    try {
        LOGI("Encoding data with protocol %d: %s", protocolId, dataToEncode);
        
        // Two-step encoding like official ggwave-java
        // Step 1: Get the required buffer size
        const int bufferSize = ggwave_encode(instance, dataToEncode, dataLength, static_cast<ggwave_ProtocolId>(protocolId), 10, nullptr, 1);
        
        if (bufferSize <= 0) {
            LOGE("Failed to get buffer size for encoding with protocol %d", protocolId);
            env->ReleaseStringUTFChars(data, dataToEncode);
            return nullptr;
        }
        
        // Step 2: Get the actual waveform data
        char* waveform = new char[bufferSize];
        const int actualSize = ggwave_encode(instance, dataToEncode, dataLength, static_cast<ggwave_ProtocolId>(protocolId), 10, waveform, 0);
        
        if (actualSize != bufferSize) {
            LOGE("Encoding size mismatch: expected %d bytes, got %d bytes", bufferSize, actualSize);
            delete[] waveform;
            env->ReleaseStringUTFChars(data, dataToEncode);
            return nullptr;
        }
        
        // Convert to byte array (I16 samples)
        jbyteArray result = env->NewByteArray(bufferSize);
        if (result == nullptr) {
            LOGE("Failed to allocate byte array");
            delete[] waveform;
            env->ReleaseStringUTFChars(data, dataToEncode);
            return nullptr;
        }
        
        env->SetByteArrayRegion(result, 0, bufferSize, reinterpret_cast<const jbyte*>(waveform));
        
        LOGI("Successfully encoded data to %d bytes (%d samples) with protocol %d", bufferSize, actualSize, protocolId);
        
        delete[] waveform;
        env->ReleaseStringUTFChars(data, dataToEncode);
        return result;
        
    } catch (const std::exception& e) {
        LOGE("Exception during encoding: %s", e.what());
        env->ReleaseStringUTFChars(data, dataToEncode);
        return nullptr;
    }
}

/**
 * Get the transmission waveform data
 */
JNIEXPORT jbyteArray JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_getTxWaveformNative(
        JNIEnv *env, jobject thiz, jlong instancePtr) {
    
    if (instancePtr < 0) {
        LOGE("Invalid GGWave instance pointer: %ld", instancePtr);
        return nullptr;
    }
    
    // Note: In C API, waveform data is returned directly from encode function
    LOGI("getTxWaveform called - data returned from encode function");
    return nullptr;
}

/**
 * Cleanup GGWave instance
 */
JNIEXPORT void JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_cleanupNative(
        JNIEnv *env, jobject thiz, jlong instancePtr) {
    
    if (instancePtr == 0) {
        LOGE("Invalid GGWave instance pointer for cleanup");
        return;
    }
    
    ggwave_Instance instance = static_cast<ggwave_Instance>(instancePtr);
    ggwave_free(instance);
    
    LOGI("GGWave instance cleaned up successfully");
}

} // extern "C"