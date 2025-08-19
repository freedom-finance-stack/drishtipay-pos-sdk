package com.freedomfinancestack.pos_sdk_core.examples;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.freedomfinancestack.pos_sdk_core.interfaces.ISoundDataTransmission;
import com.freedomfinancestack.pos_sdk_core.implementations.SoundDataTransmissionImpl;

/**
 * Simple example showing how to use sound data transmission in POS applications.
 * 
 * This demonstrates the minimalistic API - just listen(), send(), and stop().
 */
public class SimplePosExample {
    
    private static final String TAG = "SimplePosExample";
    
    private ISoundDataTransmission soundTransmission;
    
    /**
     * Initialize sound transmission for POS operations.
     * @param context Android application context
     */
    public void initialize(Context context) {
        // Create sound transmission instance
        soundTransmission = new SoundDataTransmissionImpl(context);
        Log.d(TAG, "Sound transmission initialized for POS");
    }
    
    /**
     * Start listening for payment data from customer devices.
     */
    public void startListeningForPayments() {
        soundTransmission.listen(new ISoundDataTransmission.SoundCallback() {
            @Override
            public void onReceived(@NonNull String data) {
                Log.d(TAG, "Payment data received: " + data);
                // Process payment data here
                processPaymentData(data);
            }
            
            @Override
            public void onSent(@NonNull String data) {
                // Not used when listening
            }
            
            @Override
            public void onError(@NonNull String error) {
                Log.e(TAG, "Error receiving payment data: " + error);
                // Handle error (retry, show message, etc.)
            }
        });
    }
    
    /**
     * Send payment confirmation to customer device.
     * @param confirmationData Payment confirmation or receipt data
     */
    public void sendPaymentConfirmation(String confirmationData) {
        soundTransmission.send(confirmationData, new ISoundDataTransmission.SoundCallback() {
            @Override
            public void onReceived(@NonNull String data) {
                // Not used when sending
            }
            
            @Override
            public void onSent(@NonNull String data) {
                Log.d(TAG, "Payment confirmation sent successfully");
                // Update UI to show success
            }
            
            @Override
            public void onError(@NonNull String error) {
                Log.e(TAG, "Failed to send confirmation: " + error);
                // Handle error (retry, fallback method, etc.)
            }
        });
    }
    
    /**
     * Send data without status callback (fire-and-forget).
     * @param data Data to send
     */
    public void sendQuickData(String data) {
        soundTransmission.send(data);  // Simple, no callback
    }
    
    /**
     * Stop all sound operations when done.
     */
    public void cleanup() {
        if (soundTransmission != null) {
            soundTransmission.stop();
            Log.d(TAG, "Sound transmission stopped");
        }
    }
    
    // Example payment processing
    private void processPaymentData(String data) {
        // In a real POS application, you would:
        // 1. Parse the payment data (card info, amount, etc.)
        // 2. Validate the data
        // 3. Process the payment through your payment gateway
        // 4. Send confirmation back to customer device
        
        Log.d(TAG, "Processing payment: " + data);
        
        // Example: Send confirmation after processing
        String confirmation = "Payment processed successfully - Receipt #12345";
        sendPaymentConfirmation(confirmation);
    }
}