package com.freedomfinancestack.pos_sdk_core.examples;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.freedomfinancestack.pos_sdk_core.implementations.GGWaveImpl;
import com.freedomfinancestack.pos_sdk_core.implementations.PosNfcDeviceManager;
import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWave;
import com.freedomfinancestack.pos_sdk_core.interfaces.INfcDeviceManager;
import com.freedomfinancestack.pos_sdk_core.interfaces.IPosNfcPlugin;
import com.freedomfinancestack.pos_sdk_core.models.GGWaveMessage;

/**
 * Simple example showing how to use Universal POS SDK features:
 * 1. NFC for tap-to-pay transactions
 * 2. GGWave for audio-based data transmission
 * 
 * Shows multiple approaches:
 * - Mock mode (for testing without manufacturer SDK)
 * - Plugin mode (for production with manufacturer SDK)
 * - GGWave integration for contactless audio communication
 * 
 * Works with ANY POS manufacturer: PAX, Ingenico, Verifone, etc.
 */
public class SimplePosExample extends Activity {
    
    private static final String TAG = "SimplePosExample";
    
    private INfcDeviceManager nfcManager;
    private IGGWave ggWave;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize both NFC and GGWave functionality
        
        // APPROACH 1: Mock mode (for testing without manufacturer SDK)
        setupMockMode();
        
        // APPROACH 2: Plugin mode (for production with manufacturer SDK)
        // setupPluginMode();
        
        // Initialize GGWave functionality
        setupGGWave();
        
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
    
    /**
     * Initialize GGWave functionality for audio-based communication
     */
    private void setupGGWave() {
        Log.d(TAG, "Setting up GGWave audio communication...");
        
        // Create GGWave instance with auto volume adjustment
        ggWave = new GGWaveImpl(this, true);
        
        // Initialize GGWave
        ggWave.initialize(() -> {
            Log.d(TAG, "GGWave initialized and ready");
            showMessage("Audio communication ready");
            
            // Start listening for audio messages
            startGGWaveListening();
        });
    }
    
    /**
     * Start listening for GGWave audio messages
     */
    private void startGGWaveListening() {
        if (ggWave == null || !ggWave.isInitialized()) {
            Log.w(TAG, "GGWave not ready for listening");
            return;
        }
        
        ggWave.startListening(new IGGWave.GGWaveCallback() {
            @Override
            public boolean onMessageReceived(@NonNull GGWaveMessage message) {
                Log.d(TAG, "Received DrishtiPay audio message");
                
                // Process structured DrishtiPay message  
                showMessage("DrishtiPay message received: " + message.getMobileNumber());
                
                // Continue listening for more messages
                return true;
            }
            
            @Override
            public boolean onRawMessageReceived(@NonNull String rawMessage) {
                Log.d(TAG, "Received raw audio message");
                
                // Process raw audio message
                processAudioMessage(rawMessage);
                
                // Continue listening for more messages
                return true;
            }
            
            @Override
            public void onError(@NonNull String error) {
                Log.e(TAG, "GGWave reception error: " + error);
                showMessage("Audio reception error: " + error);
            }
        });
        
        Log.d(TAG, "Started listening for audio messages");
    }
    
    /**
     * Process messages received via audio (GGWave)
     */
    private void processAudioMessage(@NonNull String message) {
        try {
            Log.d(TAG, "Processing audio message");
            
            // Check if it's a payment-related message
            if (message.toLowerCase().contains("payment") || message.startsWith("{")) {
                showMessage("Payment data received via audio");
                
                // Process as payment data
                processAudioPayment(message);
            } else {
                showMessage("General message received via audio: " + message);
                
                // Echo the message back
                sendAudioResponse("ACK: " + message);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing audio message", e);
            showMessage("Error processing audio message: " + e.getMessage());
        }
    }
    
    /**
     * Process payment data received via audio
     */
    private void processAudioPayment(@NonNull String paymentData) {
        Log.d(TAG, "Processing payment from audio");
        
        // TODO: Parse and validate payment data
        // Example: JSON parsing, amount validation, etc.
        
        // Simulate payment processing
        showMessage("Audio payment processed successfully");
        
        // Send confirmation back via audio
        sendAudioResponse("PAYMENT_CONFIRMED");
    }
    
    /**
     * Send a response message via audio
     */
    private void sendAudioResponse(@NonNull String response) {
        if (ggWave == null || !ggWave.isInitialized()) {
            Log.w(TAG, "GGWave not ready for sending");
            return;
        }
        
        ggWave.send(response, false, true, new IGGWave.GGWaveTransmissionCallback() {
            @Override
            public void onTransmissionComplete() {
                Log.d(TAG, "Audio response sent successfully");
            }
            
            @Override
            public void onTransmissionError(@NonNull String error) {
                Log.e(TAG, "Failed to send audio response: " + error);
            }
        });
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
        
        // Clean up GGWave resources
        if (ggWave != null) {
            ggWave.cleanup();
        }
        
        Log.d(TAG, "All resources cleaned up");
    }
} 