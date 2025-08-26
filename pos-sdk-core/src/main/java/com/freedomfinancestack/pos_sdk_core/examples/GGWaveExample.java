package com.freedomfinancestack.pos_sdk_core.examples;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.freedomfinancestack.pos_sdk_core.implementations.GGWaveImpl;
import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWave;
import com.freedomfinancestack.pos_sdk_core.models.GGWaveMessage;

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
     * Example of sending a DrishtiPay structured message with mobile number.
     * 
     * @param mobileNumber The mobile number to send
     */
    public void sendDrishtiPayMobile(@NonNull String mobileNumber) {
        if (ggWave == null || !ggWave.isInitialized()) {
            Log.e(TAG, "GGWave not initialized");
            return;
        }
        
        // Use the convenience method for mobile number
        boolean success = ggWave.sendMobileNumber(mobileNumber);
        
        if (success) {
            Log.d(TAG, "Started DrishtiPay mobile transmission");
        } else {
            Log.e(TAG, "Failed to start DrishtiPay mobile transmission");
        }
    }
    
    /**
     * Example of sending a custom structured DrishtiPay message.
     * 
     * @param mobileNumber The mobile number
     * @param appType Custom app type
     * @param transmissionType Custom transmission type
     */
    public void sendCustomDrishtiPayMessage(@NonNull String mobileNumber, @NonNull String appType, @NonNull String transmissionType) {
        if (ggWave == null || !ggWave.isInitialized()) {
            Log.e(TAG, "GGWave not initialized");
            return;
        }
        
        try {
            // Create custom structured message
            GGWaveMessage message = new GGWaveMessage(mobileNumber, appType, transmissionType);
            
            // Send with callback for monitoring
            boolean success = ggWave.sendMessage(
                message,
                false,  // useUltrasound - false for audible range
                true,   // fastMode - true for faster transmission
                new IGGWave.GGWaveTransmissionCallback() {
                    @Override
                    public void onTransmissionComplete() {
                        Log.d(TAG, "DrishtiPay message sent successfully");
                    }
                    
                    @Override
                    public void onTransmissionError(@NonNull String error) {
                        Log.e(TAG, "DrishtiPay transmission failed: " + error);
                    }
                }
            );
            
            if (success) {
                Log.d(TAG, "Started custom DrishtiPay transmission");
            } else {
                Log.e(TAG, "Failed to start custom DrishtiPay transmission");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating DrishtiPay message", e);
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
            public boolean onMessageReceived(@NonNull GGWaveMessage message) {
                Log.d(TAG, "Received structured DrishtiPay message: mobile=[REDACTED]");
                
                // Process the structured message
                processStructuredMessage(message);
                
                // Return true to continue listening, false to stop
                return true;
            }
            
            @Override
            public boolean onRawMessageReceived(@NonNull String rawMessage) {
                Log.d(TAG, "Received raw message: [REDACTED for privacy]");
                
                // Process raw message that doesn't match DrishtiPay format
                processRawMessage(rawMessage);
                
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
     * Process received structured DrishtiPay message.
     * 
     * @param message The parsed GGWaveMessage
     */
    private void processStructuredMessage(@NonNull GGWaveMessage message) {
        try {
            Log.d(TAG, "Processing DrishtiPay message");
            Log.d(TAG, "Mobile number: [REDACTED]"); // Don't log actual mobile
            Log.d(TAG, "App type: " + message.getAppType());
            Log.d(TAG, "Transmission type: " + message.getTransmissionType());
            
            // Validate message
            if (message.isValidDrishtiPayMessage()) {
                Log.d(TAG, "Valid DrishtiPay message format");
                // Process valid DrishtiPay message
                handleDrishtiPayMessage(message);
            } else {
                Log.w(TAG, "Invalid DrishtiPay message format");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing structured message", e);
        }
    }
    
    /**
     * Process received raw message that doesn't match DrishtiPay format.
     * 
     * @param rawMessage The raw decoded text message
     */
    private void processRawMessage(@NonNull String rawMessage) {
        try {
            Log.d(TAG, "Processing raw message");
            
            // Example: Check for other known formats
            if (rawMessage.startsWith("{") && rawMessage.contains("payment")) {
                Log.d(TAG, "Received non-DrishtiPay payment response");
                handlePaymentResponse(rawMessage);
            } else {
                Log.d(TAG, "Received general message");
                handleGeneralMessage(rawMessage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing raw message", e);
        }
    }
    
    /**
     * Handle valid DrishtiPay messages.
     * 
     * @param message The validated GGWaveMessage
     */
    private void handleDrishtiPayMessage(@NonNull GGWaveMessage message) {
        // Example implementation - handle DrishtiPay specific logic
        Log.d(TAG, "Processing DrishtiPay transaction");
        
        // TODO: Implement DrishtiPay specific handling
        // Example: Initiate payment, update customer info, etc.
        
        // Example: Send acknowledgment (mobile number hidden for privacy)
        Log.d(TAG, "Would send acknowledgment to mobile number");
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