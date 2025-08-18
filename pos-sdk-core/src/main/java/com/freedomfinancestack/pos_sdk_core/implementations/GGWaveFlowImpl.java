package com.freedomfinancestack.pos_sdk_core.implementations;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWave;
import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWaveFlow;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of IGGWaveFlow providing high-level workflow operations for GGWave.
 * 
 * This implementation manages:
 * - Device pairing workflows
 * - Data transfer sessions
 * - State management and timeouts
 * - Error recovery and retry logic
 * 
 * WORKFLOW STATES:
 * - IDLE: Ready for operations
 * - PAIRING: Currently establishing connection with peer
 * - PAIRED: Connected to a peer device
 * - TRANSFERRING: Actively transferring data
 * - ERROR: In error state, requires reset
 */
public class GGWaveFlowImpl implements IGGWaveFlow {
    
    private static final String TAG = "GGWaveFlowImpl";
    
    // Protocol constants
    private static final String PAIRING_REQUEST_PREFIX = "PAIR_REQ:";
    private static final String PAIRING_RESPONSE_PREFIX = "PAIR_RSP:";
    private static final String DATA_TRANSFER_PREFIX = "DATA:";
    private static final String DATA_REQUEST_PREFIX = "REQ:";
    private static final String ACK_PREFIX = "ACK:";
    
    private static final long DEFAULT_PAIRING_TIMEOUT = 30000; // 30 seconds
    
    private Context context;
    private IGGWave ggWave;
    private ScheduledExecutorService scheduler;
    
    // State management
    private final AtomicReference<WorkflowState> currentState = new AtomicReference<>(WorkflowState.IDLE);
    private final AtomicReference<String> pairedDeviceId = new AtomicReference<>();
    private final AtomicReference<String> myDeviceId = new AtomicReference<>();
    
    // Current operation callbacks
    private volatile PairingCallback currentPairingCallback;
    private volatile TransferCallback currentTransferCallback;
    private volatile ScheduledFuture<?> timeoutTask;
    
    /**
     * Creates a new GGWaveFlowImpl instance.
     * 
     * @param context Android application context
     * @throws IllegalArgumentException if context is null
     */
    public GGWaveFlowImpl(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        
        this.context = context.getApplicationContext();
        this.ggWave = new GGWaveImpl(this.context);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GGWaveFlow-Scheduler");
            t.setDaemon(true);
            return t;
        });
        
        Log.d(TAG, "GGWaveFlowImpl initialized");
    }
    
    @Override
    public void initiatePairing(@NonNull String deviceId, @NonNull PairingCallback callback) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        
        if (!ggWave.isInitialized()) {
            callback.onPairingFailed("GGWave not initialized");
            return;
        }
        
        if (!currentState.compareAndSet(WorkflowState.IDLE, WorkflowState.PAIRING)) {
            callback.onPairingFailed("Already in " + currentState.get() + " state");
            return;
        }
        
        myDeviceId.set(deviceId);
        currentPairingCallback = callback;
        
        Log.d(TAG, "Initiating pairing as device: " + deviceId);
        
        // Start listening for pairing responses
        ggWave.startListening(new PairingGGWaveCallback());
        
        // Send pairing request
        String pairingRequest = PAIRING_REQUEST_PREFIX + deviceId;
        ggWave.sendData(pairingRequest, new IGGWave.GGWaveCallback() {
            @Override
            public void onDataReceived(@NonNull String data) {
                // Not used in send callback
            }
            
            @Override
            public void onDataSent(@NonNull String data) {
                Log.d(TAG, "Pairing request sent successfully");
            }
            
            @Override
            public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
                Log.e(TAG, "Failed to send pairing request: " + errorMessage);
                handlePairingFailure("Failed to send pairing request: " + errorMessage);
            }
            
            @Override
            public void onTransmissionStarted() {
                Log.d(TAG, "Sending pairing request...");
            }
            
            @Override
            public void onTransmissionCompleted() {
                Log.d(TAG, "Pairing request transmission completed");
            }
        });
    }
    
    @Override
    public void waitForPairing(long timeout, @NonNull PairingCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        
        if (!ggWave.isInitialized()) {
            callback.onPairingFailed("GGWave not initialized");
            return;
        }
        
        if (!currentState.compareAndSet(WorkflowState.IDLE, WorkflowState.PAIRING)) {
            callback.onPairingFailed("Already in " + currentState.get() + " state");
            return;
        }
        
        currentPairingCallback = callback;
        long actualTimeout = timeout > 0 ? timeout : DEFAULT_PAIRING_TIMEOUT;
        
        Log.d(TAG, "Waiting for pairing requests (timeout: " + actualTimeout + "ms)");
        
        // Start listening for pairing requests
        ggWave.startListening(new PairingGGWaveCallback());
        
        // Set timeout
        timeoutTask = scheduler.schedule(() -> {
            if (currentState.get() == WorkflowState.PAIRING) {
                handlePairingTimeout();
            }
        }, actualTimeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void transferData(@NonNull String data, @NonNull TransferCallback callback) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        
        if (currentState.get() != WorkflowState.PAIRED) {
            callback.onTransferFailed("Not paired with any device");
            return;
        }
        
        if (!currentState.compareAndSet(WorkflowState.PAIRED, WorkflowState.TRANSFERRING)) {
            callback.onTransferFailed("Already transferring data");
            return;
        }
        
        currentTransferCallback = callback;
        
        Log.d(TAG, "Transferring data to paired device: " + pairedDeviceId.get());
        
        String transferData = DATA_TRANSFER_PREFIX + data;
        ggWave.sendData(transferData, new IGGWave.GGWaveCallback() {
            @Override
            public void onDataReceived(@NonNull String receivedData) {
                // Not used in send callback
            }
            
            @Override
            public void onDataSent(@NonNull String sentData) {
                Log.d(TAG, "Data transfer completed successfully");
                currentState.set(WorkflowState.PAIRED);
                callback.onTransferSuccess(data);
                currentTransferCallback = null;
            }
            
            @Override
            public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
                Log.e(TAG, "Data transfer failed: " + errorMessage);
                currentState.set(WorkflowState.PAIRED);
                callback.onTransferFailed("Transfer failed: " + errorMessage);
                currentTransferCallback = null;
            }
            
            @Override
            public void onTransmissionStarted() {
                callback.onTransferProgress(10);
            }
            
            @Override
            public void onTransmissionCompleted() {
                callback.onTransferProgress(100);
            }
        });
    }
    
    @Override
    public void transferData(@NonNull String data) {
        transferData(data, new TransferCallback() {
            @Override
            public void onTransferSuccess(@NonNull String transferredData) {
                Log.d(TAG, "Data transferred successfully (no callback)");
            }
            
            @Override
            public void onDataReceived(@NonNull String receivedData) {
                Log.d(TAG, "Data received (no callback): " + receivedData);
            }
            
            @Override
            public void onTransferFailed(@NonNull String errorMessage) {
                Log.e(TAG, "Data transfer failed (no callback): " + errorMessage);
            }
            
            @Override
            public void onTransferProgress(int progress) {
                // Silent progress updates
            }
        });
    }
    
    @Override
    public void requestData(@NonNull String requestType, @NonNull TransferCallback callback) {
        if (requestType == null || requestType.trim().isEmpty()) {
            throw new IllegalArgumentException("Request type cannot be null or empty");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        
        if (currentState.get() != WorkflowState.PAIRED) {
            callback.onTransferFailed("Not paired with any device");
            return;
        }
        
        currentTransferCallback = callback;
        
        Log.d(TAG, "Requesting data from paired device: " + requestType);
        
        String dataRequest = DATA_REQUEST_PREFIX + requestType;
        ggWave.sendData(dataRequest);
    }
    
    @Override
    public void cancelOperation() {
        Log.d(TAG, "Cancelling current operation");
        
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(true);
        }
        
        ggWave.stopListening();
        
        WorkflowState state = currentState.get();
        if (state == WorkflowState.PAIRING && currentPairingCallback != null) {
            currentPairingCallback.onPairingFailed("Operation cancelled");
        } else if (state == WorkflowState.TRANSFERRING && currentTransferCallback != null) {
            currentTransferCallback.onTransferFailed("Transfer cancelled");
        }
        
        resetToIdleState();
    }
    
    @Override
    public boolean isPaired() {
        return currentState.get() == WorkflowState.PAIRED && pairedDeviceId.get() != null;
    }
    
    @Override
    @Nullable
    public String getPairedDeviceId() {
        return pairedDeviceId.get();
    }
    
    @Override
    public void unpair() {
        Log.d(TAG, "Unpairing from device: " + pairedDeviceId.get());
        
        pairedDeviceId.set(null);
        ggWave.stopListening();
        currentState.set(WorkflowState.IDLE);
    }
    
    @Override
    @NonNull
    public WorkflowState getState() {
        return currentState.get();
    }
    
    @Override
    public void cleanup() {
        Log.d(TAG, "Cleaning up GGWaveFlow resources...");
        
        cancelOperation();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        if (ggWave != null) {
            ggWave.cleanup();
        }
        
        Log.d(TAG, "GGWaveFlow cleanup completed");
    }
    
    private void handlePairingFailure(String errorMessage) {
        if (currentPairingCallback != null) {
            currentPairingCallback.onPairingFailed(errorMessage);
        }
        resetToIdleState();
    }
    
    private void handlePairingTimeout() {
        Log.d(TAG, "Pairing timeout occurred");
        if (currentPairingCallback != null) {
            currentPairingCallback.onPairingTimeout();
        }
        resetToIdleState();
    }
    
    private void resetToIdleState() {
        currentState.set(WorkflowState.IDLE);
        currentPairingCallback = null;
        currentTransferCallback = null;
        ggWave.stopListening();
    }
    
    /**
     * GGWave callback handler for pairing operations.
     */
    private class PairingGGWaveCallback implements IGGWave.GGWaveCallback {
        
        @Override
        public void onDataReceived(@NonNull String data) {
            Log.d(TAG, "Received pairing data: " + data);
            
            if (data.startsWith(PAIRING_REQUEST_PREFIX)) {
                handlePairingRequest(data.substring(PAIRING_REQUEST_PREFIX.length()));
            } else if (data.startsWith(PAIRING_RESPONSE_PREFIX)) {
                handlePairingResponse(data.substring(PAIRING_RESPONSE_PREFIX.length()));
            } else if (data.startsWith(DATA_TRANSFER_PREFIX)) {
                handleDataTransfer(data.substring(DATA_TRANSFER_PREFIX.length()));
            } else if (data.startsWith(DATA_REQUEST_PREFIX)) {
                handleDataRequest(data.substring(DATA_REQUEST_PREFIX.length()));
            }
        }
        
        @Override
        public void onDataSent(@NonNull String data) {
            Log.d(TAG, "Pairing data sent: " + data);
        }
        
        @Override
        public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
            Log.e(TAG, "GGWave error during pairing: " + errorMessage);
            handlePairingFailure("GGWave error: " + errorMessage);
        }
        
        @Override
        public void onTransmissionStarted() {
            Log.d(TAG, "Pairing transmission started");
        }
        
        @Override
        public void onTransmissionCompleted() {
            Log.d(TAG, "Pairing transmission completed");
        }
    }
    
    private void handlePairingRequest(String requestingDeviceId) {
        Log.d(TAG, "Received pairing request from: " + requestingDeviceId);
        
        if (currentPairingCallback != null) {
            PairingResponse response = new PairingResponse() {
                @Override
                public void accept() {
                    Log.d(TAG, "Accepting pairing request from: " + requestingDeviceId);
                    
                    pairedDeviceId.set(requestingDeviceId);
                    currentState.set(WorkflowState.PAIRED);
                    
                    // Send pairing response
                    String pairingResponse = PAIRING_RESPONSE_PREFIX + myDeviceId.get();
                    ggWave.sendData(pairingResponse);
                    
                    if (currentPairingCallback != null) {
                        currentPairingCallback.onPairingSuccess(requestingDeviceId);
                        currentPairingCallback = null;
                    }
                    
                    cancelTimeout();
                }
                
                @Override
                public void reject(@Nullable String reason) {
                    Log.d(TAG, "Rejecting pairing request from: " + requestingDeviceId + 
                          (reason != null ? " (" + reason + ")" : ""));
                    // Continue waiting for other pairing requests
                }
            };
            
            currentPairingCallback.onPairingRequest(requestingDeviceId, response);
        }
    }
    
    private void handlePairingResponse(String respondingDeviceId) {
        Log.d(TAG, "Received pairing response from: " + respondingDeviceId);
        
        if (currentState.get() == WorkflowState.PAIRING) {
            pairedDeviceId.set(respondingDeviceId);
            currentState.set(WorkflowState.PAIRED);
            
            if (currentPairingCallback != null) {
                currentPairingCallback.onPairingSuccess(respondingDeviceId);
                currentPairingCallback = null;
            }
            
            cancelTimeout();
        }
    }
    
    private void handleDataTransfer(String transferredData) {
        Log.d(TAG, "Received data transfer: " + transferredData);
        
        if (currentTransferCallback != null) {
            currentTransferCallback.onDataReceived(transferredData);
        }
    }
    
    private void handleDataRequest(String requestType) {
        Log.d(TAG, "Received data request: " + requestType);
        
        if (currentTransferCallback != null) {
            // In a real implementation, this would handle the specific request type
            // For now, just notify about the request
            currentTransferCallback.onDataReceived("REQUEST:" + requestType);
        }
    }
    
    private void cancelTimeout() {
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(true);
            timeoutTask = null;
        }
    }
}