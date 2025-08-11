package com.freedomfinancestack.pos_sdk_core.interfaces;

import android.content.Context;

/**
 * Plugin interface for POS NFC implementations.
 * 
 * Organizations implement this interface to integrate their specific
 * POS hardware SDK (PAX, Ingenico, etc.) with our core SDK.
 */
public interface IPosNfcPlugin {
    
    /**
     * Initialize the plugin with context
     * @param context Android application context
     * @throws Exception if initialization fails
     */
    void initialize(Context context) throws Exception;
    
    /**
     * Start listening for NFC taps
     * @param callback Callback to receive NFC events
     * @throws Exception if start fails
     */
    void startListening(INfcDeviceManager.NdefCallback callback) throws Exception;
    
    /**
     * Stop listening for NFC taps
     * @throws Exception if stop fails
     */
    void stopListening() throws Exception;
    
    /**
     * Check if currently listening
     * @return true if listening, false otherwise
     */
    boolean isListening();
    
    /**
     * Get plugin information
     * @return plugin name and version
     */
    String getPluginInfo();
    
    /**
     * Get supported device types
     * @return list of supported POS devices
     */
    String getSupportedDevices();
    
    /**
     * Clean up resources
     */
    void cleanup();
} 