package com.freedomfinancestack.pos_sdk_core.implementations;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freedomfinancestack.pos_sdk_core.interfaces.ISoundDataTransmission;
import com.freedomfinancestack.pos_sdk_core.interfaces.ISoundBasedDeviceWorkflow;
import com.freedomfinancestack.pos_sdk_core.constants.GGWaveConstants;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of ISoundBasedDeviceWorkflow providing high-level workflow operations for sound-based device communication.
 * 
 * This implementation manages:
 * - Device pairing workflows via sound transmission
 * - Data transfer sessions between paired devices
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
public class SoundBasedDeviceWorkflowImpl implements ISoundBasedDeviceWorkflow {
    
    private static final String TAG = "SoundBasedDeviceWorkflowImpl";
    
    private Context context;
    private ISoundDataTransmission soundDataTransmission;
    private ScheduledExecutorService scheduler;
    
    // State management
    private final AtomicReference<WorkflowState> currentState = new AtomicReference<>(WorkflowState.IDLE);
    private final AtomicReference<String> pairedDeviceId = new AtomicReference<>();
    private final AtomicReference<String> myDeviceId = new AtomicReference<>();
    
    // Current operation callbacks
    private volatile DevicePairingCallback currentPairingCallback;
    private volatile DataTransferCallback currentTransferCallback;
    private volatile ScheduledFuture<?> timeoutTask;
    
    /**
     * Creates a new SoundBasedDeviceWorkflowImpl instance.
     * 
     * @param context Android application context
     * @throws IllegalArgumentException if context is null
     */
    public SoundBasedDeviceWorkflowImpl(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        
        this.context = context.getApplicationContext();
        this.soundDataTransmission = new SoundDataTransmissionImpl(this.context);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, GGWaveConstants.SCHEDULER_THREAD_NAME);
            t.setDaemon(true);
            return t;
        });
        
        Log.d(TAG, "SoundBasedDeviceWorkflowImpl initialized");
    }
    
    @Override
    public void initiatePairing(@NonNull String deviceId, @NonNull DevicePairingCallback callback) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        
        if (!soundDataTransmission.isInitialized()) {
            callback.onPairingFailed("Sound data transmission not initialized");
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
        soundDataTransmission.startListening(new PairingGGWaveCallback());
        
        // Send pairing request
        String pairingRequest = GGWaveConstants.PAIRING_REQUEST_PREFIX + deviceId;
        soundDataTransmission.sendData(pairingRequest, new ISoundDataTransmission.SoundTransmissionCallback() {
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
    public void waitForPairing(long timeout, @NonNull DevicePairingCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        
        if (!soundDataTransmission.isInitialized()) {
            callback.onPairingFailed("Sound data transmission not initialized");
            return;
        }
        
        if (!currentState.compareAndSet(WorkflowState.IDLE, WorkflowState.PAIRING)) {
            callback.onPairingFailed("Already in " + currentState.get() + " state");
            return;
        }
        
        currentPairingCallback = callback;
        long actualTimeout = timeout > 0 ? timeout : GGWaveConstants.DEFAULT_PAIRING_TIMEOUT_MS;
        
        Log.d(TAG, "Waiting for pairing requests (timeout: " + actualTimeout + "ms)");
        
        // Start listening for pairing requests
        soundDataTransmission.startListening(new PairingGGWaveCallback());
        
        // Set timeout
        timeoutTask = scheduler.schedule(() -> {
            if (currentState.get() == WorkflowState.PAIRING) {
                handlePairingTimeout();
            }
        }, actualTimeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void transferData(@NonNull String data, @NonNull DataTransferCallback callback) {
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
        
        String transferData = GGWaveConstants.DATA_TRANSFER_PREFIX + data;
        soundDataTransmission.sendData(transferData, new ISoundDataTransmission.SoundTransmissionCallback() {
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
        transferData(data, new DataTransferCallback() {
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
    public void requestData(@NonNull String requestType, @NonNull DataTransferCallback callback) {
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
        
        String dataRequest = GGWaveConstants.DATA_REQUEST_PREFIX + requestType;
        soundDataTransmission.sendData(dataRequest);
    }
    
    @Override
    public void cancelOperation() {
        Log.d(TAG, "Cancelling current operation");
        
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(true);
        }
        
        soundDataTransmission.stopListening();
        
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
        soundDataTransmission.stopListening();
        currentState.set(WorkflowState.IDLE);
    }
    
    @Override
    @NonNull
    public WorkflowState getState() {
        return currentState.get();
    }
    
    @Override
    public void cleanup() {
        Log.d(TAG, "Cleaning up SoundBasedDeviceWorkflow resources...");
        
        cancelOperation();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        if (soundDataTransmission != null) {
            soundDataTransmission.cleanup();
        }
        
        Log.d(TAG, "SoundBasedDeviceWorkflow cleanup completed");
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
        soundDataTransmission.stopListening();
    }
    
    /**
     * Sound transmission callback handler for pairing operations.
     */
    private class PairingGGWaveCallback implements ISoundDataTransmission.SoundTransmissionCallback {
        
        @Override
        public void onDataReceived(@NonNull String data) {
            Log.d(TAG, "Received pairing data: " + data);
            
            if (data.startsWith(GGWaveConstants.PAIRING_REQUEST_PREFIX)) {
                handlePairingRequest(data.substring(GGWaveConstants.PAIRING_REQUEST_PREFIX.length()));
            } else if (data.startsWith(GGWaveConstants.PAIRING_RESPONSE_PREFIX)) {
                handlePairingResponse(data.substring(GGWaveConstants.PAIRING_RESPONSE_PREFIX.length()));
            } else if (data.startsWith(GGWaveConstants.DATA_TRANSFER_PREFIX)) {
                handleDataTransfer(data.substring(GGWaveConstants.DATA_TRANSFER_PREFIX.length()));
            } else if (data.startsWith(GGWaveConstants.DATA_REQUEST_PREFIX)) {
                handleDataRequest(data.substring(GGWaveConstants.DATA_REQUEST_PREFIX.length()));
            }
        }
        
        @Override
        public void onDataSent(@NonNull String data) {
            Log.d(TAG, "Pairing data sent: " + data);
        }
        
        @Override
        public void onError(@NonNull String errorMessage, @Nullable Exception exception) {
            Log.e(TAG, "Sound transmission error during pairing: " + errorMessage);
            handlePairingFailure("Sound transmission error: " + errorMessage);
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
                    String pairingResponse = GGWaveConstants.PAIRING_RESPONSE_PREFIX + myDeviceId.get();
                    soundDataTransmission.sendData(pairingResponse);
                    
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