package com.freedomfinancestack.pos_sdk_core.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface for GGWave sound-based data transmission functionality.
 * 
 * GGWave allows data transmission through sound waves, enabling communication
 * between devices without requiring network connectivity or physical contact.
 * 
 * USAGE:
 * 1. Initialize with proper audio permissions
 * 2. Start listening for incoming data
 * 3. Send data when needed
 * 4. Handle callbacks on appropriate threads
 * 
 * THREADING: All callbacks are invoked on background threads.
 * UI updates must be dispatched to main thread by the caller.
 */
public interface IGGWave {

    /**
     * Starts listening for incoming GGWave sound transmissions.
     * This is a non-blocking operation that will invoke callbacks when data is received.
     * 
     * @param callback The callback to handle received data and errors.
     *                Must not be null.
     * @throws IllegalArgumentException if callback is null
     * @throws IllegalStateException if audio permissions are not granted
     */
    void startListening(@NonNull GGWaveCallback callback);

    /**
     * Stops listening for GGWave transmissions.
     * This operation is idempotent and safe to call multiple times.
     */
    void stopListening();

    /**
     * Sends data via GGWave sound transmission.
     * This is a non-blocking operation.
     * 
     * @param data The data to transmit. Must not be null or empty.
     * @param callback Optional callback to handle transmission status.
     * @throws IllegalArgumentException if data is null or empty
     * @throws IllegalStateException if not properly initialized
     */
    void sendData(@NonNull String data, @Nullable GGWaveCallback callback);

    /**
     * Sends data via GGWave sound transmission without status callback.
     * 
     * @param data The data to transmit. Must not be null or empty.
     * @throws IllegalArgumentException if data is null or empty
     * @throws IllegalStateException if not properly initialized
     */
    void sendData(@NonNull String data);

    /**
     * Checks if currently listening for GGWave transmissions.
     * 
     * @return true if listening, false otherwise
     */
    boolean isListening();

    /**
     * Checks if GGWave is properly initialized and ready for use.
     * 
     * @return true if initialized, false otherwise
     */
    boolean isInitialized();

    /**
     * Gets the current volume level used for transmission.
     * 
     * @return volume level between 0.0 and 1.0
     */
    float getTransmissionVolume();

    /**
     * Sets the volume level for GGWave transmissions.
     * 
     * @param volume Volume level between 0.0 and 1.0
     * @throws IllegalArgumentException if volume is outside valid range
     */
    void setTransmissionVolume(float volume);

    /**
     * Cleans up resources and stops all operations.
     * Should be called when GGWave is no longer needed.
     */
    void cleanup();

    /**
     * Callback interface for GGWave operations.
     * All methods are called on background threads.
     */
    interface GGWaveCallback {
        /**
         * Called when data is successfully received via GGWave.
         * 
         * @param data The received data. Never null.
         */
        void onDataReceived(@NonNull String data);

        /**
         * Called when data is successfully sent via GGWave.
         * 
         * @param data The data that was sent. Never null.
         */
        void onDataSent(@NonNull String data);

        /**
         * Called when an error occurs during GGWave operations.
         * 
         * @param errorMessage Description of the error. Never null.
         * @param exception Optional exception that caused the error.
         */
        void onError(@NonNull String errorMessage, @Nullable Exception exception);

        /**
         * Called when transmission starts.
         */
        void onTransmissionStarted();

        /**
         * Called when transmission completes (success or failure).
         */
        void onTransmissionCompleted();
    }
}
