package com.freedomfinancestack.pos_sdk_core.implementations;

import android.content.Context;
import android.nfc.NdefMessage;
import android.util.Log;

import com.freedomfinancestack.pos_sdk_core.interfaces.INfcDeviceManager;
import com.freedomfinancestack.pos_sdk_core.interfaces.IPosNfcPlugin;

/**
 * Universal POS Terminal NFC implementation using plugin architecture.
 * 
 * This implementation works with ANY POS manufacturer (PAX, Ingenico, Verifone, etc.)
 * by delegating hardware communication to manufacturer-specific plugins.
 * 
 * SETUP FOR ORGANIZATIONS:
 * 1. Get SDK from your POS manufacturer (PAX Neptune Lite, Ingenico, etc.)
 * 2. Create implementation of IPosNfcPlugin for your hardware
 * 3. Pass plugin to this constructor
 * 
 * SUPPORTED MANUFACTURERS:
 * - PAX (A920, A930, Neptune Lite, etc.)
 * - Ingenico (iCT220, iCT250, etc.) 
 * - Verifone (VX series)
 * - Any manufacturer with custom plugin
 */
public class PosNfcDeviceManager implements INfcDeviceManager {
    
    private static final String TAG = "PosNfcManager";
    
    private Context context;
    private NdefCallback currentCallback;
    private IPosNfcPlugin plugin;
    private boolean isInitialized = false;
    
    /**
     * Constructor that accepts a POS NFC plugin
     * @param context Android application context
     * @param plugin Implementation of IPosNfcPlugin (contains manufacturer SDK integration)
     */
    public PosNfcDeviceManager(Context context, IPosNfcPlugin plugin) {
        this.context = context;
        this.plugin = plugin;
        
        if (plugin == null) {
            throw new IllegalArgumentException("POS NFC Plugin cannot be null. " +
                "Organizations must provide their own manufacturer SDK implementation.");
        }
        
        initializePlugin();
        Log.d(TAG, "POS NFC Manager initialized with plugin: " + plugin.getPluginInfo());
    }
    
    /**
     * Alternative constructor for mock/testing (no plugin required)
     * @param context Android application context
     */
    public PosNfcDeviceManager(Context context) {
        this.context = context;
        this.plugin = new MockPosPlugin(); // Use mock for testing
        initializePlugin();
        Log.d(TAG, "POS NFC Manager initialized with MOCK plugin for testing");
    }
    
    private void initializePlugin() {
        try {
            plugin.initialize(context);
            isInitialized = true;
            Log.d(TAG, "Plugin initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize plugin", e);
            isInitialized = false;
        }
    }
    
    @Override
    public void startListening(NdefCallback callback) {
        if (!isInitialized) {
            Log.e(TAG, "Plugin not initialized");
            if (callback != null) {
                callback.onError("POS NFC plugin not initialized");
            }
            return;
        }
        
        if (callback == null) {
            Log.e(TAG, "Callback cannot be null");
            return;
        }
        
        this.currentCallback = callback;
        Log.d(TAG, "Starting NFC listening via plugin...");
        
        try {
            plugin.startListening(callback);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start listening via plugin", e);
            callback.onError("Failed to start NFC: " + e.getMessage());
        }
    }
    
    @Override
    public void stopListening() {
        Log.d(TAG, "Stopping NFC listening via plugin...");
        
        try {
            if (plugin != null) {
                plugin.stopListening();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping NFC via plugin", e);
        }
        
        this.currentCallback = null;
    }
    
    /**
     * Check if currently listening for NFC
     */
    public boolean isListening() {
        return plugin != null && plugin.isListening();
    }
    
    /**
     * Get information about the current plugin
     */
    public String getPluginInfo() {
        return plugin != null ? plugin.getPluginInfo() : "No plugin loaded";
    }
    
    /**
     * Get supported devices from the plugin
     */
    public String getSupportedDevices() {
        return plugin != null ? plugin.getSupportedDevices() : "Unknown";
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        stopListening();
        if (plugin != null) {
            plugin.cleanup();
        }
    }
    
    /**
     * Mock plugin for testing when real manufacturer SDK is not available
     */
    private static class MockPosPlugin implements IPosNfcPlugin {
        private boolean listening = false;
        
        @Override
        public void initialize(Context context) throws Exception {
            Log.d(TAG, "Mock POS plugin initialized");
        }
        
        @Override
        public void startListening(INfcDeviceManager.NdefCallback callback) throws Exception {
            listening = true;
            Log.d(TAG, "Mock: Starting NFC listening");
            
            // Simulate NFC tap after 3 seconds for testing
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    if (listening) {
                        Log.d(TAG, "Mock: Simulating NFC tap");
                        // callback.onNdefMessageDiscovered(mockMessage);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        
        @Override
        public void stopListening() throws Exception {
            listening = false;
            Log.d(TAG, "Mock: Stopping NFC listening");
        }
        
        @Override
        public boolean isListening() {
            return listening;
        }
        
        @Override
        public String getPluginInfo() {
            return "Mock POS Plugin v1.0 (for testing - supports any manufacturer)";
        }
        
        @Override
        public String getSupportedDevices() {
            return "Mock devices: PAX A920/A930, Ingenico iCT220, Verifone VX820 (testing)";
        }
        
        @Override
        public void cleanup() {
            listening = false;
            Log.d(TAG, "Mock: Plugin cleaned up");
        }
    }
} 