package com.freedomfinancestack.pos_sdk_core.implementations;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWave;

/**
 * Default implementation of IGGWave interface using GGWaveManager.
 * 
 * This class provides a simple facade over GGWaveManager for backward compatibility
 * and easy usage in POS SDK applications.
 * 
 * Example usage:
 * <pre>
 * IGGWave ggWave = new GGWaveImpl(context);
 * ggWave.initialize(() -> {
 *     // Ready to use
 *     ggWave.send("Hello World");
 * });
 * </pre>
 */
public class GGWaveImpl implements IGGWave {
    
    private static final String TAG = "GGWaveImpl";
    
    private final GGWaveManager manager;
    
    /**
     * Creates a new GGWaveImpl with default settings.
     * 
     * @param context Application context, must not be null
     */
    public GGWaveImpl(@NonNull Context context) {
        this(context, false);
    }
    
    /**
     * Creates a new GGWaveImpl with custom volume adjustment setting.
     * 
     * @param context Application context, must not be null
     * @param autoAdjustVolume Whether to automatically adjust volume during transmission
     */
    public GGWaveImpl(@NonNull Context context, boolean autoAdjustVolume) {
        this.manager = new GGWaveManager(context, autoAdjustVolume);
    }
    
    @Override
    public void initialize(@Nullable Runnable readyCallback) {
        manager.initialize(readyCallback);
    }
    
    @Override
    public boolean send(@NonNull String message, boolean useUltrasound, boolean fastMode, @Nullable GGWaveTransmissionCallback callback) {
        return manager.send(message, useUltrasound, fastMode, callback);
    }
    
    @Override
    public boolean send(@NonNull String message) {
        return manager.send(message);
    }
    
    @Override
    public boolean startListening(@NonNull GGWaveCallback callback) {
        return manager.startListening(callback);
    }
    
    @Override
    public void stopListening() {
        manager.stopListening();
    }
    
    @Override
    public boolean isListening() {
        return manager.isListening();
    }
    
    @Override
    public boolean isInitialized() {
        return manager.isInitialized();
    }
    
    @Override
    public void cleanup() {
        manager.cleanup();
    }
}
