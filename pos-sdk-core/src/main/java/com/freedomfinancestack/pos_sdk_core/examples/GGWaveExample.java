package com.freedomfinancestack.pos_sdk_core.examples;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freedomfinancestack.pos_sdk_core.implementations.GGWaveFlowImpl;
import com.freedomfinancestack.pos_sdk_core.implementations.GGWaveImpl;
import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWave;
import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWaveFlow;

/**
 * Comprehensive example showing how to use GGWave for sound-based data transmission.
 * 
 * Demonstrates three approaches:
 * 1. Basic GGWave usage (low-level API)
 * 2. Workflow-based usage (high-level API)
 * 3. POS payment data transfer scenarios
 * 
 * REQUIREMENTS:
 * - RECORD_AUDIO permission in AndroidManifest.xml
 * - Device with microphone and speakers
 * - Two devices for testing (sender and receiver)
 */
public class GGWaveExample extends Activity {
    
    private static final String TAG = "GGWaveExample";
    
    private IGGWave ggWave;
    private IGGWaveFlow ggWaveFlow;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize GGWave components
        setupGGWave();
        
        // Choose demo approach:
        
        // APPROACH 1: Basic GGWave usage
        demonstrateBasicUsage();
        
        // APPROACH 2: Workflow-based usage
        // demonstrateWorkflowUsage();
        
        // APPROACH 3: POS payment scenarios
        // demonstratePaymentScenarios();
    }
    
    /**
     * Initialize GGWave components
     */
    private void setupGGWave() {
        Log.d(TAG, "Setting up GGWave components...");
        
        // Basic GGWave instance
        ggWave = new GGWaveImpl(this);
        
        // High-level workflow instance
        ggWaveFlow = new GGWaveFlowImpl(this);
        
        showMessage("GGWave initialized successfully");
    }
    
    /**
     * APPROACH 1: Basic GGWave usage for simple data transmission
     */
    private void demonstrateBasicUsage() {
        Log.d(TAG, "=== BASIC GGWAVE USAGE DEMO ===");
        
        // Example 1: Sending data
        sendDataExample();
        
        // Example 2: Listening for data
        listenForDataExample();
    }
    
    private void sendDataExample() {
        Log.d(TAG, "--- Sending Data Example ---");
        
        String dataToSend = "Hello from POS terminal!";
        
        ggWave.sendData(dataToSend, new IGGWave.GGWaveCallback() {
            @Override
            public void onDataReceived(@NonNull String data) {
                // Not used in send callback
            }
            
            @Override
            public void onDataSent(@NonNull String data) {
                Log.d(TAG, "Data sent successfully: " + data);
                showMessage("Data sent: " + data);
            }
            
            @Override
            public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
                Log.e(TAG, "Send error: " + errorMessage);
                showMessage("Send failed: " + errorMessage);
            }
            
            @Override
            public void onTransmissionStarted() {
                Log.d(TAG, "Starting transmission...");
                showMessage("Transmitting data via sound...");
            }
            
            @Override
            public void onTransmissionCompleted() {
                Log.d(TAG, "Transmission completed");
            }
        });
    }
    
    private void listenForDataExample() {
        Log.d(TAG, "--- Listening for Data Example ---");
        
        ggWave.startListening(new IGGWave.GGWaveCallback() {
            @Override
            public void onDataReceived(@NonNull String data) {
                Log.d(TAG, "Data received: " + data);
                showMessage("Received: " + data);
                
                // Stop listening after receiving data
                ggWave.stopListening();
            }
            
            @Override
            public void onDataSent(@NonNull String data) {
                // Not used in receive callback
            }
            
            @Override
            public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
                Log.e(TAG, "Receive error: " + errorMessage);
                showMessage("Receive failed: " + errorMessage);
            }
            
            @Override
            public void onTransmissionStarted() {
                Log.d(TAG, "Listening for transmissions...");
            }
            
            @Override
            public void onTransmissionCompleted() {
                Log.d(TAG, "Reception completed");
            }
        });
        
        showMessage("Listening for sound-based data...");
    }
    
    /**
     * APPROACH 2: Workflow-based usage for device pairing and data transfer
     */
    private void demonstrateWorkflowUsage() {
        Log.d(TAG, "=== WORKFLOW USAGE DEMO ===");
        
        // Example: Device pairing workflow
        devicePairingExample();
    }
    
    private void devicePairingExample() {
        Log.d(TAG, "--- Device Pairing Example ---");
        
        String myDeviceId = "POS_TERMINAL_001";
        
        // Initiate pairing
        ggWaveFlow.initiatePairing(myDeviceId, new IGGWaveFlow.PairingCallback() {
            @Override
            public void onPairingSuccess(@NonNull String deviceId) {
                Log.d(TAG, "Successfully paired with: " + deviceId);
                showMessage("Paired with device: " + deviceId);
                
                // Now we can transfer data
                transferDataToPairedDevice();
            }
            
            @Override
            public void onPairingRequest(@NonNull String deviceId, @NonNull IGGWaveFlow.PairingResponse response) {
                Log.d(TAG, "Pairing request from: " + deviceId);
                showMessage("Pairing request from: " + deviceId);
                
                // Auto-accept for demo (in real app, show user dialog)
                response.accept();
            }
            
            @Override
            public void onPairingFailed(@NonNull String errorMessage) {
                Log.e(TAG, "Pairing failed: " + errorMessage);
                showMessage("Pairing failed: " + errorMessage);
            }
            
            @Override
            public void onPairingTimeout() {
                Log.d(TAG, "Pairing timed out");
                showMessage("Pairing timeout - no devices found");
            }
        });
        
        showMessage("Initiating device pairing...");
    }
    
    private void transferDataToPairedDevice() {
        Log.d(TAG, "--- Data Transfer to Paired Device ---");
        
        String paymentData = "{\"amount\":\"25.99\",\"currency\":\"USD\",\"merchant\":\"Coffee Shop\"}";
        
        ggWaveFlow.transferData(paymentData, new IGGWaveFlow.TransferCallback() {
            @Override
            public void onTransferSuccess(@NonNull String data) {
                Log.d(TAG, "Data transfer successful: " + data);
                showMessage("Payment data sent successfully");
            }
            
            @Override
            public void onDataReceived(@NonNull String data) {
                Log.d(TAG, "Received response data: " + data);
                showMessage("Received: " + data);
            }
            
            @Override
            public void onTransferFailed(@NonNull String errorMessage) {
                Log.e(TAG, "Transfer failed: " + errorMessage);
                showMessage("Transfer failed: " + errorMessage);
            }
            
            @Override
            public void onTransferProgress(int progress) {
                Log.d(TAG, "Transfer progress: " + progress + "%");
                if (progress == 100) {
                    showMessage("Transfer completed");
                }
            }
        });
    }
    
    /**
     * APPROACH 3: POS payment scenarios using GGWave
     */
    private void demonstratePaymentScenarios() {
        Log.d(TAG, "=== PAYMENT SCENARIOS DEMO ===");
        
        // Scenario 1: Send payment request to customer device
        sendPaymentRequestExample();
        
        // Scenario 2: Receive payment confirmation
        receivePaymentConfirmationExample();
    }
    
    private void sendPaymentRequestExample() {
        Log.d(TAG, "--- Send Payment Request Example ---");
        
        // Create payment request data
        String paymentRequest = createPaymentRequest("25.99", "USD", "Coffee Shop", "ORDER_123");
        
        ggWave.setTransmissionVolume(0.8f); // Higher volume for payment scenarios
        
        ggWave.sendData(paymentRequest, new IGGWave.GGWaveCallback() {
            @Override
            public void onDataReceived(@NonNull String data) {
                // Not used in send callback
            }
            
            @Override
            public void onDataSent(@NonNull String data) {
                Log.d(TAG, "Payment request sent to customer");
                showMessage("Payment request sent - waiting for customer response");
                
                // Start listening for payment confirmation
                listenForPaymentConfirmation();
            }
            
            @Override
            public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
                Log.e(TAG, "Failed to send payment request: " + errorMessage);
                showMessage("Payment request failed: " + errorMessage);
            }
            
            @Override
            public void onTransmissionStarted() {
                showMessage("Sending payment request via sound...");
            }
            
            @Override
            public void onTransmissionCompleted() {
                Log.d(TAG, "Payment request transmission completed");
            }
        });
    }
    
    private void listenForPaymentConfirmation() {
        Log.d(TAG, "--- Listen for Payment Confirmation ---");
        
        ggWave.startListening(new IGGWave.GGWaveCallback() {
            @Override
            public void onDataReceived(@NonNull String data) {
                Log.d(TAG, "Received payment data: " + data);
                
                if (isPaymentConfirmation(data)) {
                    processPaymentConfirmation(data);
                } else {
                    Log.w(TAG, "Received non-payment data: " + data);
                }
            }
            
            @Override
            public void onDataSent(@NonNull String data) {
                // Not used in receive callback
            }
            
            @Override
            public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
                Log.e(TAG, "Error receiving payment confirmation: " + errorMessage);
                showMessage("Payment confirmation error: " + errorMessage);
            }
            
            @Override
            public void onTransmissionStarted() {
                showMessage("Listening for payment confirmation...");
            }
            
            @Override
            public void onTransmissionCompleted() {
                Log.d(TAG, "Payment confirmation reception completed");
            }
        });
    }
    
    private void receivePaymentConfirmationExample() {
        // This would be implemented in a customer-facing app
        Log.d(TAG, "This example would run on customer device to confirm payment");
    }
    
    // Helper methods for payment scenarios
    
    private String createPaymentRequest(String amount, String currency, String merchant, String orderId) {
        return String.format(
            "{\"type\":\"payment_request\",\"amount\":\"%s\",\"currency\":\"%s\",\"merchant\":\"%s\",\"order_id\":\"%s\",\"timestamp\":%d}",
            amount, currency, merchant, orderId, System.currentTimeMillis()
        );
    }
    
    private boolean isPaymentConfirmation(String data) {
        return data.contains("\"type\":\"payment_confirmation\"") || 
               data.contains("\"status\":\"approved\"");
    }
    
    private void processPaymentConfirmation(String confirmationData) {
        Log.d(TAG, "Processing payment confirmation: " + confirmationData);
        
        // Parse confirmation data
        // In real implementation, validate signature, check amount, etc.
        
        showMessage("Payment confirmed! Transaction completed.");
        
        // Stop listening after successful payment
        ggWave.stopListening();
    }
    
    // Utility methods
    
    private void showMessage(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "Message: " + message);
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        Log.d(TAG, "Cleaning up GGWave resources...");
        
        // Clean up GGWave resources
        if (ggWave != null) {
            ggWave.cleanup();
        }
        
        if (ggWaveFlow != null) {
            ggWaveFlow.cleanup();
        }
        
        Log.d(TAG, "GGWave cleanup completed");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Stop listening when app goes to background
        if (ggWave != null && ggWave.isListening()) {
            ggWave.stopListening();
        }
        
        if (ggWaveFlow != null) {
            ggWaveFlow.cancelOperation();
        }
    }
}