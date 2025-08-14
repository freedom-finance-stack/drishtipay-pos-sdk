package com.freedomfinancestack.razorpay_drishtipay_test.showcase;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.freedomfinancestack.pos_sdk_core.implementations.PosNfcDeviceManager;
import com.freedomfinancestack.pos_sdk_core.interfaces.INfcDeviceManager;

/**
 * Public Showcase Activity for DrishtiPay POS SDK
 * 
 * This activity demonstrates the SDK capabilities without exposing any
 * proprietary manufacturer details. Perfect for:
 * 
 * - Sales presentations
 * - Public demonstrations
 * - Partner integration training
 * - Marketing showcases
 * - Technical evaluations
 * 
 * Features:
 * - Professional presentation interface
 * - Multiple payment scenario simulations
 * - Real-time status monitoring
 * - No proprietary SDK dependencies
 * - Safe for public distribution
 */
public class ShowcaseActivity extends Activity {
    
    private static final String TAG = "ShowcaseActivity";
    
    private INfcDeviceManager nfcManager;
    private AbstractPosPlugin showcasePlugin;
    private TextView statusText;
    private TextView paymentDataText;
    private TextView logText;
    private ScrollView logScrollView;
    private LinearLayout buttonLayout;
    
    private boolean isListening = false;
    private StringBuilder logBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        createShowcaseUI();
        initializeShowcaseSDK();
        
        Log.d(TAG, "Showcase Activity started - ready for public demonstration");
    }
    
    private void createShowcaseUI() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);
        
        // Title
        TextView titleText = new TextView(this);
        titleText.setText("DrishtiPay POS SDK - Public Showcase");
        titleText.setTextSize(24);
        titleText.setPadding(0, 0, 0, 32);
        mainLayout.addView(titleText);
        
        // Status Section
        TextView statusLabel = new TextView(this);
        statusLabel.setText("SDK Status:");
        statusLabel.setTextSize(18);
        statusLabel.setPadding(0, 16, 0, 8);
        mainLayout.addView(statusLabel);
        
        statusText = new TextView(this);
        statusText.setText("Initializing...");
        statusText.setPadding(16, 8, 0, 16);
        statusText.setBackgroundColor(0xFFF5F5F5);
        mainLayout.addView(statusText);
        
        // Control Buttons
        buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        buttonLayout.setPadding(0, 16, 0, 16);
        
        createControlButtons();
        mainLayout.addView(buttonLayout);
        
        // Payment Data Section
        TextView paymentLabel = new TextView(this);
        paymentLabel.setText("Last Payment Data:");
        paymentLabel.setTextSize(18);
        paymentLabel.setPadding(0, 16, 0, 8);
        mainLayout.addView(paymentLabel);
        
        paymentDataText = new TextView(this);
        paymentDataText.setText("No payments processed yet");
        paymentDataText.setPadding(16, 8, 16, 16);
        paymentDataText.setBackgroundColor(0xFFF0F8FF);
        paymentDataText.setMaxLines(10);
        mainLayout.addView(paymentDataText);
        
        // Log Section
        TextView logLabel = new TextView(this);
        logLabel.setText("Activity Log:");
        logLabel.setTextSize(18);
        logLabel.setPadding(0, 16, 0, 8);
        mainLayout.addView(logLabel);
        
        logText = new TextView(this);
        logText.setPadding(16, 8, 16, 16);
        logText.setBackgroundColor(0xFF2F2F2F);
        logText.setTextColor(0xFF00FF00);
        logText.setTextSize(12);
        
        logScrollView = new ScrollView(this);
        logScrollView.addView(logText);
        logScrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400));
        mainLayout.addView(logScrollView);
        
        setContentView(new ScrollView(this) {{ addView(mainLayout); }});
    }
    
    private void createControlButtons() {
        // Main control buttons
        createButton("Start Payment Listening", v -> startPaymentListening());
        createButton("Stop Listening", v -> stopPaymentListening());
        
        // Scenario simulation buttons
        TextView scenarioLabel = new TextView(this);
        scenarioLabel.setText("Payment Scenarios (for demonstration):");
        scenarioLabel.setTextSize(16);
        scenarioLabel.setPadding(0, 16, 0, 8);
        buttonLayout.addView(scenarioLabel);
        
        createButton("💳 Credit Card Payment", v -> simulateScenario(AbstractPosPlugin.PaymentScenario.CREDIT_CARD));
        createButton("💳 Debit Card Payment", v -> simulateScenario(AbstractPosPlugin.PaymentScenario.DEBIT_CARD));
        createButton("📱 UPI Payment", v -> simulateScenario(AbstractPosPlugin.PaymentScenario.UPI_PAYMENT));
        createButton("📶 Contactless Payment", v -> simulateScenario(AbstractPosPlugin.PaymentScenario.CONTACTLESS_PAYMENT));
        createButton("❌ Payment Error", v -> simulateScenario(AbstractPosPlugin.PaymentScenario.PAYMENT_ERROR));
        
        // Utility buttons
        createButton("Clear Logs", v -> clearLogs());
    }
    
    private void createButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(listener);
        button.setPadding(16, 16, 16, 16);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        button.setLayoutParams(params);
        
        buttonLayout.addView(button);
    }
    
    private void initializeShowcaseSDK() {
        try {
            addLog("🚀 Initializing DrishtiPay POS SDK for public showcase...");
            
            // Create abstract plugin for public demonstration
            showcasePlugin = new AbstractPosPlugin();
            showcasePlugin.configureSimulatedDevice("Professional POS", "Enterprise Terminal");
            showcasePlugin.setRealisticSimulation(true, 3000);
            
            // Initialize the POS NFC Device Manager with showcase plugin
            nfcManager = new PosNfcDeviceManager(this, showcasePlugin);
            
            updateStatus("✅ SDK Initialized Successfully\n" +
                       "Plugin: " + showcasePlugin.getPluginInfo() + "\n" +
                       "Ready for public demonstration");
            
            addLog("✅ DrishtiPay POS SDK initialized successfully!");
            addLog("🎯 Configured for public showcase - no proprietary dependencies");
            addLog("📋 Supported devices: " + showcasePlugin.getSupportedDevices());
            
        } catch (Exception e) {
            updateStatus("❌ SDK Initialization Failed: " + e.getMessage());
            addLog("❌ Failed to initialize showcase SDK: " + e.getMessage());
            Log.e(TAG, "Failed to initialize showcase SDK", e);
        }
    }
    
    private void startPaymentListening() {
        if (isListening) {
            showToast("Already listening for payments");
            return;
        }
        
        try {
            addLog("🎧 Starting payment listening for showcase...");
            
            nfcManager.startListening(new INfcDeviceManager.NdefCallback() {
                @Override
                public void onNdefMessageDiscovered(NdefMessage message) {
                    runOnUiThread(() -> {
                        addLog("💳 Payment detected! Processing showcase transaction...");
                        processShowcasePayment(message);
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        addLog("❌ Payment Error: " + errorMessage);
                        showToast("Payment Error: " + errorMessage);
                    });
                }
            });
            
            isListening = true;
            updateStatus("🎧 Listening for payments...\n" +
                       "Ready to demonstrate payment processing\n" +
                       "Tap any scenario button or wait for auto-simulation");
            
            addLog("✅ Payment listening started - ready for customer demonstrations");
            showToast("Ready for payment demonstration!");
            
        } catch (Exception e) {
            addLog("❌ Failed to start payment listening: " + e.getMessage());
            showToast("Failed to start listening: " + e.getMessage());
        }
    }
    
    private void stopPaymentListening() {
        if (!isListening) {
            showToast("Not currently listening");
            return;
        }
        
        try {
            addLog("🛑 Stopping payment listening...");
            nfcManager.stopListening();
            isListening = false;
            
            updateStatus("⏹️ Payment listening stopped\n" +
                       "Click 'Start Payment Listening' to resume demonstration");
            
            addLog("✅ Payment listening stopped");
            showToast("Payment listening stopped");
            
        } catch (Exception e) {
            addLog("❌ Error stopping payment listening: " + e.getMessage());
        }
    }
    
    private void simulateScenario(AbstractPosPlugin.PaymentScenario scenario) {
        if (!isListening) {
            showToast("Please start payment listening first");
            return;
        }
        
        addLog("🎯 Demonstrating payment scenario: " + scenario.name());
        showcasePlugin.simulatePaymentScenario(scenario);
    }
    
    private void processShowcasePayment(NdefMessage message) {
        try {
            // Extract payment data for demonstration
            String paymentData = extractShowcasePaymentData(message);
            
            addLog("💰 Processing showcase payment...");
            paymentDataText.setText(paymentData);
            
            // Simulate realistic payment processing for demonstration
            addLog("🔄 Sending to payment gateway (simulated)...");
            
            // Simulate processing time for realistic demonstration
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Simulate gateway processing time
                    runOnUiThread(() -> {
                        addLog("✅ Payment processed successfully! (Demonstration completed)");
                        updateStatus("✅ Payment Successful!\n" +
                                   "Transaction processed in demonstration mode\n" +
                                   "Ready for next payment simulation");
                        
                        showToast("Payment Successful! (Demo Mode)");
                        
                        // Auto-stop listening after successful demo payment
                        stopPaymentListening();
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } catch (Exception e) {
            addLog("❌ Payment processing failed: " + e.getMessage());
            showToast("Payment processing failed: " + e.getMessage());
        }
    }
    
    private String extractShowcasePaymentData(NdefMessage message) {
        try {
            if (message.getRecords().length > 0) {
                byte[] payload = message.getRecords()[0].getPayload();
                String rawData = new String(payload);
                
                // Format for better display in showcase
                return "DEMONSTRATION PAYMENT DATA:\n\n" + 
                       formatJsonForDisplay(rawData) + 
                       "\n\n⚠️ This is simulated data for demonstration purposes only";
            } else {
                return "No payment data found in demonstration";
            }
        } catch (Exception e) {
            return "Error extracting demo payment data: " + e.getMessage();
        }
    }
    
    private String formatJsonForDisplay(String json) {
        // Simple JSON formatting for better readability in demonstrations
        return json.replace("{", "{\n  ")
                  .replace(",", ",\n  ")
                  .replace("}", "\n}");
    }
    
    private void clearLogs() {
        logBuffer.setLength(0);
        logText.setText("");
        addLog("📋 Logs cleared - ready for new demonstration");
    }
    
    private void addLog(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";
        
        logBuffer.append(logEntry);
        
        runOnUiThread(() -> {
            logText.setText(logBuffer.toString());
            // Auto-scroll to bottom
            logScrollView.post(() -> logScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
        
        Log.d(TAG, message);
    }
    
    private void updateStatus(String status) {
        runOnUiThread(() -> statusText.setText(status));
    }
    
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        try {
            if (nfcManager != null) {
                nfcManager.stopListening();
                if (nfcManager instanceof PosNfcDeviceManager) {
                    ((PosNfcDeviceManager) nfcManager).cleanup();
                }
            }
            addLog("🧹 Showcase SDK resources cleaned up");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during showcase cleanup", e);
        }
    }
}
