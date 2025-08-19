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
 * Process captured audio data and decode any messages
 */
JNIEXPORT jstring JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_processCaptureDataNative(
        JNIEnv *env, jobject thiz, jlong instancePtr, jshortArray audioData) {
    
    if (instancePtr == 0) {
        LOGE("Invalid GGWave instance pointer: %ld", instancePtr);
        return nullptr;
    }
    
    ggwave_Instance instance = static_cast<ggwave_Instance>(instancePtr);
    
    jsize dataSize = env->GetArrayLength(audioData);
    jboolean isCopy = false;
    jshort* cData = env->GetShortArrayElements(audioData, &isCopy);
    
    if (cData == nullptr) {
        LOGE("Failed to get audio data array");
        return nullptr;
    }
    
    try {
        char output[256];
        int ret = ggwave_decode(instance, (char*)cData, 2 * dataSize, output);
        
        env->ReleaseShortArrayElements(audioData, cData, JNI_ABORT);
        
        if (ret > 0) {
            LOGI("Decoded message: '%s'", output);
            return env->NewStringUTF(output);
        }
        
        return nullptr;
        
    } catch (const std::exception& e) {
        LOGE("Exception during audio processing: %s", e.what());
        env->ReleaseShortArrayElements(audioData, cData, JNI_ABORT);
        return nullptr;
    }
}

/**
 * Encode message to audio samples (like official ggwave-java)
 */
JNIEXPORT jshortArray JNICALL
Java_com_freedomfinancestack_pos_1sdk_1core_implementations_SoundDataTransmissionImpl_sendMessageNative(
        JNIEnv *env, jobject thiz, jlong instancePtr, jstring message) {
    
    if (instancePtr == 0) {
        LOGE("Invalid GGWave instance pointer: %ld", instancePtr);
        return nullptr;
    }
    
    ggwave_Instance instance = static_cast<ggwave_Instance>(instancePtr);
    
    const char* messageToEncode = env->GetStringUTFChars(message, nullptr);
    int messageLength = env->GetStringLength(message);
    
    if (messageToEncode == nullptr) {
        LOGE("Failed to get message string");
        return nullptr;
    }
    
    try {
        LOGI("Encoding message: %s", messageToEncode);
        
        // Use AUDIBLE_FAST protocol like the working example
        const int protocolId = GGWAVE_PROTOCOL_AUDIBLE_FAST;
        
        // Two-step encoding like official ggwave-java
        // Step 1: Get the required buffer size
        const int bufferSize = ggwave_encode(instance, messageToEncode, messageLength, 
                                           static_cast<ggwave_ProtocolId>(protocolId), 10, nullptr, 1);
        
        if (bufferSize <= 0) {
            LOGE("Failed to get buffer size for encoding");
            env->ReleaseStringUTFChars(message, messageToEncode);
            return nullptr;
        }
        
        // Step 2: Get the actual waveform data
        char* waveform = new char[bufferSize];
        const int actualSamples = ggwave_encode(instance, messageToEncode, messageLength, 
                                              static_cast<ggwave_ProtocolId>(protocolId), 10, waveform, 0);
        
        if (2 * actualSamples != bufferSize) {
            LOGE("Encoding size mismatch: expected %d samples (%d bytes), got %d samples", 
                 bufferSize/2, bufferSize, actualSamples);
            delete[] waveform;
            env->ReleaseStringUTFChars(message, messageToEncode);
            return nullptr;
        }
        
        // Convert to short array (I16 samples)
        jshortArray result = env->NewShortArray(actualSamples);
        if (result == nullptr) {
            LOGE("Failed to allocate short array");
            delete[] waveform;
            env->ReleaseStringUTFChars(message, messageToEncode);
            return nullptr;
        }
        
        env->SetShortArrayRegion(result, 0, actualSamples, reinterpret_cast<const jshort*>(waveform));
        
        LOGI("Successfully encoded message to %d audio samples", actualSamples);
        
        delete[] waveform;
        env->ReleaseStringUTFChars(message, messageToEncode);
        return result;
        
    } catch (const std::exception& e) {
        LOGE("Exception during message encoding: %s", e.what());
        env->ReleaseStringUTFChars(message, messageToEncode);
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
        LOGE("Invalid GGWave instance pointer for cleanup");
        return;
    }
    
    ggwave_Instance instance = static_cast<ggwave_Instance>(instancePtr);
    ggwave_free(instance);
    
    LOGI("GGWave instance cleaned up successfully");
}

} // extern "C"