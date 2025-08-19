package com.freedomfinancestack.pos_sdk_core.implementations;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freedomfinancestack.pos_sdk_core.interfaces.ISoundDataTransmission;
import com.freedomfinancestack.pos_sdk_core.constants.GGWaveConstants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sound-based data transmission using GGWave technology.
 * Optimized for POS systems with inaudible ultrasound communication.
 */
public class SoundDataTransmissionImpl implements ISoundDataTransmission {
    
    private static final String TAG = "SoundDataTransmission";
    
    private final Context context;
    private final ExecutorService executor;
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    
    // Native library instance
    private long nativeInstance = 0;
    
    // Current callback reference
    private SoundCallback currentCallback;
    
    /**
     * Create new sound transmission instance.
     * @param context Android context for audio access
     */
    public SoundDataTransmissionImpl(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, GGWaveConstants.THREAD_NAME_PREFIX);
            t.setDaemon(true);
            return t;
        });
        
        initialize();
    }

    @Override
    public void listen(@NonNull SoundCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        
        if (isListening.compareAndSet(false, true)) {
            currentCallback = callback;
            executor.execute(() -> {
                try {
                    startListeningNative(nativeInstance);
                    // Start capture loop
                    captureLoop();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start listening", e);
                    isListening.set(false);
                    if (currentCallback != null) {
                        currentCallback.onError("Failed to start listening: " + e.getMessage());
                    }
                }
            });
            Log.d(TAG, "Started listening for sound data");
        } else {
            Log.w(TAG, "Already listening - ignoring duplicate request");
        }
    }

    @Override
    public void send(@NonNull String data, @Nullable SoundCallback callback) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        
        if (data.length() > GGWaveConstants.MAX_DATA_LENGTH) {
            throw new IllegalArgumentException("Data too long (max " + GGWaveConstants.MAX_DATA_LENGTH + " chars)");
        }
        
        executor.execute(() -> {
            try {
                if (nativeInstance < 0) {
                    Log.e(TAG, "Native instance not initialized: " + nativeInstance);
                    if (callback != null) {
                        callback.onError("Native instance not initialized");
                    }
                    return;
                }
                
                Log.d(TAG, "Sending data: " + truncateForLog(data));
                
                byte[] audioData = encodeToAudioWithProtocolNative(
                    nativeInstance, 
                    data, 
                    GGWaveConstants.DEFAULT_TX_PROTOCOL_ID
                );
                
                if (audioData != null && audioData.length > 0) {
                    // Audio data generated successfully
                    if (callback != null) {
                        callback.onSent(data);
                    }
                    Log.d(TAG, "Data sent successfully");
                } else {
                    String error = "Failed to encode data";
                    Log.e(TAG, error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            } catch (Exception e) {
                String error = "Send failed: " + e.getMessage();
                Log.e(TAG, error, e);
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    @Override
    public void send(@NonNull String data) {
        send(data, null);
    }

    @Override
    public void stop() {
        if (isListening.compareAndSet(true, false)) {
            try {
                stopListeningNative(nativeInstance);
                currentCallback = null;
                Log.d(TAG, "Stopped listening");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping listening", e);
            }
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        
        if (nativeInstance != 0) {
            cleanupNative(nativeInstance);
            nativeInstance = 0;
        }
    }
    
    // Private methods
    
    private void initialize() {
        try {
            nativeInstance = initializeNative(
                GGWaveConstants.SAMPLE_RATE, 
                GGWaveConstants.SAMPLES_PER_FRAME
            );
            
            if (nativeInstance < 0) {
                throw new RuntimeException("Native initialization returned invalid instance: " + nativeInstance);
            }
            
            Log.d(TAG, "GGWave initialized successfully with instance: " + nativeInstance);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GGWave", e);
            throw new RuntimeException("GGWave initialization failed", e);
        }
    }
    
    private void captureLoop() {
        while (isListening.get() && currentCallback != null) {
            try {
                String receivedData = captureAndDecodeNative(nativeInstance);
                if (receivedData != null && !receivedData.trim().isEmpty()) {
                    Log.d(TAG, "Received data: " + truncateForLog(receivedData));
                    currentCallback.onReceived(receivedData.trim());
                }
                
                // Small delay to prevent busy waiting
                Thread.sleep(GGWaveConstants.AUDIO_CAPTURE_DELAY_MS);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error in capture loop", e);
                if (currentCallback != null) {
                    currentCallback.onError("Capture error: " + e.getMessage());
                }
                break;
            }
        }
        isListening.set(false);
    }
    
    private String truncateForLog(String data) {
        if (data == null) return "null";
        return data.length() > GGWaveConstants.LOG_DATA_TRUNCATE_LENGTH 
            ? data.substring(0, GGWaveConstants.LOG_DATA_TRUNCATE_LENGTH) + "..."
            : data;
    }
    
    // Native method declarations - JNI calls to GGWave library
    private native long initializeNative(int sampleRate, int samplesPerFrame);
    private native void startListeningNative(long instance);
    private native void stopListeningNative(long instance);
    private native String captureAndDecodeNative(long instance);
    private native byte[] encodeToAudioWithProtocolNative(long instance, String data, int protocolId);
    private native void cleanupNative(long instance);
    
    static {
        try {
            // Load the GGWave native library
            System.loadLibrary("pos_sdk_ggwave");
            Log.d(TAG, "GGWave native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load GGWave native library", e);
            throw e;
        }
    }
}