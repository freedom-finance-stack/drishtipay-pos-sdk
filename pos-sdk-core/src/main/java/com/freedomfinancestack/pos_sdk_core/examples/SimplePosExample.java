package com.freedomfinancestack.pos_sdk_core.examples;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.freedomfinancestack.pos_sdk_core.implementations.PosNfcDeviceManager;
import com.freedomfinancestack.pos_sdk_core.interfaces.INfcDeviceManager;
import com.freedomfinancestack.pos_sdk_core.interfaces.IPosNfcPlugin;

/**
 * Simple example showing how to use Universal POS NFC for payments.
 * 
 * Shows two approaches:
 * 1. Mock mode (for testing without manufacturer SDK)
 * 2. Plugin mode (for production with manufacturer SDK)
 * 
 * Works with ANY POS manufacturer: PAX, Ingenico, Verifone, etc.
 */
public class SimplePosExample extends Activity {
    
    private static final String TAG = "SimplePosExample";
    
    private INfcDeviceManager nfcManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Choose approach based on your setup:
        
        // APPROACH 1: Mock mode (for testing without manufacturer SDK)
        setupMockMode();
        
        // APPROACH 2: Plugin mode (for production with manufacturer SDK)
        // setupPluginMode();
        
        // Start payment session
        startPayment();
    }
    
    /**
     * APPROACH 1: Mock mode for testing without manufacturer SDK
     */
    private void setupMockMode() {
        Log.d(TAG, "Setting up MOCK mode (no manufacturer SDK required)");
        
        // Simple constructor - uses built-in mock plugin
        nfcManager = new PosNfcDeviceManager(this);
    }
    
    /**
     * APPROACH 2: Plugin mode for production with manufacturer SDK
     * Organizations uncomment this and provide their own plugin
     */
    private void setupPluginMode() {
        Log.d(TAG, "Setting up PLUGIN mode (requires manufacturer SDK)");
        
        // TODO: Organizations replace this with their actual plugin
        /*
        // Example with PAX Neptune Lite plugin:
        IPosNfcPlugin paxPlugin = new NeptuneLitePlugin();
        nfcManager = new PosNfcDeviceManager(this, paxPlugin);
        
        // Example with Ingenico plugin:
        IPosNfcPlugin ingenicoPlugin = new IngenicoPlugin();
        nfcManager = new PosNfcDeviceManager(this, ingenicoPlugin);
        
        // Example with Verifone plugin:
        IPosNfcPlugin verifonePlugin = new VerifonePlugin();
        nfcManager = new PosNfcDeviceManager(this, verifonePlugin);
        */
        
        // For this example, we'll use mock mode
        nfcManager = new PosNfcDeviceManager(this);
        
        showMessage("Plugin mode setup complete. Replace with real plugin for production.");
    }
    
    private void startPayment() {
        Log.d(TAG, "Starting payment session...");
        
        nfcManager.startListening(new INfcDeviceManager.NdefCallback() {
            @Override
            public void onNdefMessageDiscovered(NdefMessage message) {
                Log.d(TAG, "Customer tapped phone - processing payment...");
                processPayment(message);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "NFC Error: " + errorMessage);
                showMessage("Payment failed: " + errorMessage);
            }
        });
        
        showMessage("Ready for customer to tap phone...");
    }
    
    private void processPayment(NdefMessage message) {
        try {
            // Extract payment data from NDEF message
            String paymentData = extractPaymentData(message);
            
            Log.d(TAG, "Processing payment: " + paymentData);
            
            // TODO: Process payment with your payment gateway
            
            // Simulate successful payment
            showMessage("Payment successful! Amount processed.");
            
            // Stop listening after successful payment
            nfcManager.stopListening();
            
        } catch (Exception e) {
            Log.e(TAG, "Payment processing failed", e);
            showMessage("Payment processing failed: " + e.getMessage());
        }
    }
    
    private String extractPaymentData(NdefMessage message) {
        // Simple extraction - in real app, parse according to your protocol
        if (message.getRecords().length > 0) {
            byte[] payload = message.getRecords()[0].getPayload();
            return new String(payload);
        }
        return "No payment data found";
    }
    
    private void showMessage(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "Message: " + message);
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Always clean up NFC resources
        if (nfcManager != null) {
            nfcManager.stopListening();
            
            // Clean up plugin resources if using PosNfcDeviceManager
            if (nfcManager instanceof PosNfcDeviceManager) {
                ((PosNfcDeviceManager) nfcManager).cleanup();
            }
        }
    }
} 