package com.freedomfinancestack.pos_sdk_core.examples;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.freedomfinancestack.pos_sdk_core.implementations.GGWaveImpl;
import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWave;

/**
 * Example demonstrating how to use GGWave functionality in the POS SDK.
 * 
 * This example shows:
 * 1. How to initialize GGWave
 * 2. How to send data over audio
 * 3. How to receive data from audio
 * 4. Proper lifecycle management
 */
public class GGWaveExample {
    
    private static final String TAG = "GGWaveExample";
    
    private IGGWave ggWave;
    private Context context;
    
    /**
     * Initialize the GGWave example.
     * 
     * @param context Application context
     */
    public void initialize(@NonNull Context context) {
        this.context = context;
        
        // Create GGWave instance with auto volume adjustment
        ggWave = new GGWaveImpl(context, true);
        
        // Initialize and wait for ready callback
        ggWave.initialize(() -> {
            Log.d(TAG, "GGWave is ready to use!");
            
            // Example: Send a test message
            sendTestMessage();
        });
    }
    
    /**
     * Example of sending a message over audio.
     */
    public void sendTestMessage() {
        if (ggWave == null || !ggWave.isInitialized()) {
            Log.e(TAG, "GGWave not initialized");
            return;
        }
        
        String testMessage = "Hello from POS SDK!";
        
        // Send with transmission callback
        boolean success = ggWave.send(
            testMessage,
            false,  // useUltrasound - false for audible range
            true,   // fastMode - true for faster transmission
            new IGGWave.GGWaveTransmissionCallback() {
                @Override
                public void onTransmissionComplete() {
                    Log.d(TAG, "Message transmission completed successfully");
                }
                
                @Override
                public void onTransmissionError(@NonNull String error) {
                    Log.e(TAG, "Transmission failed: " + error);
                }
            }
        );
        
        if (success) {
            Log.d(TAG, "Message transmission started");
        } else {
            Log.e(TAG, "Failed to start message transmission");
        }
    }
    
    /**
     * Example of sending a payment request over audio.
     * 
     * @param paymentData JSON string containing payment information
     */
    public void sendPaymentRequest(@NonNull String paymentData) {
        if (ggWave == null || !ggWave.isInitialized()) {
            Log.e(TAG, "GGWave not initialized");
            return;
        }
        
        // Send payment data using ultrasound for privacy
        boolean success = ggWave.send(
            paymentData,
            true,   // useUltrasound - true for privacy (less audible)
            false,  // fastMode - false for reliability
            new IGGWave.GGWaveTransmissionCallback() {
                @Override
                public void onTransmissionComplete() {
                    Log.d(TAG, "Payment request sent successfully");
                    // Handle successful payment transmission
                }
                
                @Override
                public void onTransmissionError(@NonNull String error) {
                    Log.e(TAG, "Payment transmission failed: " + error);
                    // Handle payment transmission failure
                }
            }
        );
        
        if (!success) {
            Log.e(TAG, "Failed to start payment transmission");
        }
    }
    
    /**
     * Example of listening for incoming audio messages.
     */
    public void startListening() {
        if (ggWave == null || !ggWave.isInitialized()) {
            Log.e(TAG, "GGWave not initialized");
            return;
        }
        
        boolean success = ggWave.startListening(new IGGWave.GGWaveCallback() {
            @Override
            public boolean onMessageReceived(@NonNull String message) {
                Log.d(TAG, "Received message: [REDACTED for privacy]");
                
                // Process the received message
                processReceivedMessage(message);
                
                // Return true to continue listening, false to stop
                return true;
            }
            
            @Override
            public void onError(@NonNull String error) {
                Log.e(TAG, "Reception error: " + error);
                // Handle reception error
            }
        });
        
        if (success) {
            Log.d(TAG, "Started listening for messages");
        } else {
            Log.e(TAG, "Failed to start listening");
        }
    }
    
    /**
     * Process a received message (example implementation).
     * 
     * @param message The received message
     */
    private void processReceivedMessage(@NonNull String message) {
        try {
            // Example: Check if it's a payment response
            if (message.startsWith("{") && message.contains("payment")) {
                Log.d(TAG, "Received payment response");
                // Process payment response JSON
                handlePaymentResponse(message);
            } else {
                Log.d(TAG, "Received general message");
                // Process general message
                handleGeneralMessage(message);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing received message", e);
        }
    }
    
    /**
     * Handle payment response messages.
     * 
     * @param paymentJson JSON string containing payment response
     */
    private void handlePaymentResponse(@NonNull String paymentJson) {
        // Example implementation - parse and handle payment response
        Log.d(TAG, "Processing payment response");
        
        // TODO: Parse JSON and update payment status
        // Example: Update UI, notify payment completion, etc.
    }
    
    /**
     * Handle general text messages.
     * 
     * @param message The received text message
     */
    private void handleGeneralMessage(@NonNull String message) {
        // Example implementation - handle general messages
        Log.d(TAG, "Processing general message");
        
        // TODO: Handle general message (notifications, status updates, etc.)
    }
    
    /**
     * Stop listening for messages.
     */
    public void stopListening() {
        if (ggWave != null) {
            ggWave.stopListening();
            Log.d(TAG, "Stopped listening for messages");
        }
    }
    
    /**
     * Check if currently listening for messages.
     * 
     * @return true if listening, false otherwise
     */
    public boolean isListening() {
        return ggWave != null && ggWave.isListening();
    }
    
    /**
     * Example of sending a simple message with default settings.
     * 
     * @param message The message to send
     */
    public void sendSimpleMessage(@NonNull String message) {
        if (ggWave == null || !ggWave.isInitialized()) {
            Log.e(TAG, "GGWave not initialized");
            return;
        }
        
        // Send with default settings (audible, fast mode, no callback)
        boolean success = ggWave.send(message);
        
        if (success) {
            Log.d(TAG, "Simple message sent");
        } else {
            Log.e(TAG, "Failed to send simple message");
        }
    }
    
    /**
     * Clean up resources when done.
     * Call this when the GGWave functionality is no longer needed.
     */
    public void cleanup() {
        if (ggWave != null) {
            ggWave.cleanup();
            ggWave = null;
            Log.d(TAG, "GGWave resources cleaned up");
        }
    }
    
    /**
     * Get the current initialization status.
     * 
     * @return true if initialized and ready, false otherwise
     */
    public boolean isReady() {
        return ggWave != null && ggWave.isInitialized();
    }
}