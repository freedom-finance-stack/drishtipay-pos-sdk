package com.freedomfinancestack.pos_sdk_core.examples;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.freedomfinancestack.pos_sdk_core.implementations.PaxNfcDeviceManager;
import com.freedomfinancestack.pos_sdk_core.interfaces.INfcDeviceManager;

/**
 * Simple example showing how to use PAX NFC for payments.
 * This is as simple as it gets!
 */
public class SimplePaxExample extends Activity {
    
    private static final String TAG = "SimplePaxExample";
    
    private INfcDeviceManager nfcManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Step 1: Create PAX NFC manager
        nfcManager = new PaxNfcDeviceManager(this);
        
        // Step 2: Start payment session
        startPayment();
    }
    
    private void startPayment() {
        Log.d(TAG, "Starting payment session...");
        
        // Step 3: Start listening for customer's phone tap
        nfcManager.startListening(new INfcDeviceManager.NdefCallback() {
            @Override
            public void onNdefMessageDiscovered(NdefMessage message) {
                // Customer tapped their phone!
                Log.d(TAG, "Customer tapped phone - processing payment...");
                
                // Process the payment data
                processPayment(message);
            }
            
            @Override
            public void onError(String errorMessage) {
                // Something went wrong
                Log.e(TAG, "NFC Error: " + errorMessage);
                showMessage("Payment failed: " + errorMessage);
            }
        });
        
        showMessage("Ready for customer to tap phone...");
    }
    
    private void processPayment(NdefMessage message) {
        // Extract payment info from NFC message
        // Process with your payment gateway
        // Show success/failure
        
        Log.d(TAG, "Payment processed successfully!");
        showMessage("Payment successful!");
        
        // Stop listening after successful payment
        nfcManager.stopListening();
    }
    
    private void showMessage(String message) {
        runOnUiThread(() -> 
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        );
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nfcManager != null) {
            nfcManager.stopListening();
        }
    }
} 