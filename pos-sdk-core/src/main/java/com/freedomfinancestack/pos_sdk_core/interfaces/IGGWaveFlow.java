package com.freedomfinancestack.pos_sdk_core.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * High-level interface for GGWave workflow operations.
 * 
 * This interface provides simplified methods for common GGWave use cases
 * such as device pairing and data transfer workflows. It internally uses
 * the core IGGWave implementation.
 * 
 * TYPICAL WORKFLOW:
 * 1. initiatePairing() - Start pairing process
 * 2. waitForPairing() - Wait for peer device to pair
 * 3. transferData() - Send data to paired device
 * 4. cleanup() - Clean up resources
 */
public interface IGGWaveFlow {

    /**
     * Initiates a device pairing process using GGWave.
     * This will start listening for pairing requests and broadcast pairing availability.
     * 
     * @param deviceId Unique identifier for this device
     * @param callback Callback to handle pairing events
     * @throws IllegalArgumentException if deviceId is null or empty
     * @throws IllegalStateException if not properly initialized
     */
    void initiatePairing(@NonNull String deviceId, @NonNull PairingCallback callback);

    /**
     * Waits for a pairing request from another device.
     * This is typically called on the receiving device.
     * 
     * @param timeout Timeout in milliseconds (0 for no timeout)
     * @param callback Callback to handle pairing events
     */
    void waitForPairing(long timeout, @NonNull PairingCallback callback);

    /**
     * Transfers data to a previously paired device.
     * 
     * @param data The data to transfer
     * @param callback Callback to handle transfer status
     * @throws IllegalArgumentException if data is null or empty
     * @throws IllegalStateException if no device is paired
     */
    void transferData(@NonNull String data, @NonNull TransferCallback callback);

    /**
     * Transfers data to a previously paired device without callback.
     * 
     * @param data The data to transfer
     * @throws IllegalArgumentException if data is null or empty
     * @throws IllegalStateException if no device is paired
     */
    void transferData(@NonNull String data);

    /**
     * Requests data from a paired device.
     * 
     * @param requestType Type of data being requested
     * @param callback Callback to handle the response
     * @throws IllegalStateException if no device is paired
     */
    void requestData(@NonNull String requestType, @NonNull TransferCallback callback);

    /**
     * Cancels any ongoing pairing or transfer operation.
     */
    void cancelOperation();

    /**
     * Checks if a device is currently paired.
     * 
     * @return true if paired with another device
     */
    boolean isPaired();

    /**
     * Gets the ID of the currently paired device.
     * 
     * @return paired device ID, or null if not paired
     */
    @Nullable
    String getPairedDeviceId();

    /**
     * Unpairs from the current device.
     */
    void unpair();

    /**
     * Gets the current workflow state.
     * 
     * @return current state of the GGWave workflow
     */
    @NonNull
    WorkflowState getState();

    /**
     * Cleans up resources and stops all operations.
     */
    void cleanup();

    /**
     * Callback interface for pairing operations.
     */
    interface PairingCallback {
        /**
         * Called when pairing is successfully established.
         * 
         * @param deviceId ID of the paired device
         */
        void onPairingSuccess(@NonNull String deviceId);

        /**
         * Called when a pairing request is received.
         * 
         * @param deviceId ID of the requesting device
         * @param acceptPairing Callback to accept or reject the pairing
         */
        void onPairingRequest(@NonNull String deviceId, @NonNull PairingResponse acceptPairing);

        /**
         * Called when pairing fails.
         * 
         * @param errorMessage Description of the error
         */
        void onPairingFailed(@NonNull String errorMessage);

        /**
         * Called when pairing times out.
         */
        void onPairingTimeout();
    }

    /**
     * Callback interface for data transfer operations.
     */
    interface TransferCallback {
        /**
         * Called when data transfer is successful.
         * 
         * @param data The transferred data
         */
        void onTransferSuccess(@NonNull String data);

        /**
         * Called when data is received from paired device.
         * 
         * @param data The received data
         */
        void onDataReceived(@NonNull String data);

        /**
         * Called when data transfer fails.
         * 
         * @param errorMessage Description of the error
         */
        void onTransferFailed(@NonNull String errorMessage);

        /**
         * Called to report transfer progress.
         * 
         * @param progress Progress percentage (0-100)
         */
        void onTransferProgress(int progress);
    }

    /**
     * Interface for responding to pairing requests.
     */
    interface PairingResponse {
        /**
         * Accepts the pairing request.
         */
        void accept();

        /**
         * Rejects the pairing request.
         * 
         * @param reason Optional reason for rejection
         */
        void reject(@Nullable String reason);
    }

    /**
     * Enumeration of possible workflow states.
     */
    enum WorkflowState {
        IDLE,           // Not performing any operation
        PAIRING,        // Currently pairing with another device
        PAIRED,         // Successfully paired with a device
        TRANSFERRING,   // Currently transferring data
        ERROR           // In error state
    }
}
