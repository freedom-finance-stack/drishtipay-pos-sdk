package com.freedomfinancestack.pos_sdk_core.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface for sound-based data transmission functionality using GGWave technology.
 * 
 * This interface provides methods for transmitting and receiving data through sound waves,
 * enabling communication between devices without requiring network connectivity or physical contact.
 * 
 * USAGE:
 * 1. Initialize with proper audio permissions (RECORD_AUDIO)
 * 2. Start listening for incoming data transmissions
 * 3. Send data when needed via sound waves
 * 4. Handle callbacks on appropriate threads
 * 
 * THREADING: All callbacks are invoked on background threads.
 * UI updates must be dispatched to main thread by the caller.
 */
public interface ISoundDataTransmission {

    /**
     * Starts listening for incoming sound transmissions.
     * This is a non-blocking operation that will invoke callbacks when data is received.
     * 
     * @param callback The callback to handle received data and errors.
     *                Must not be null.
     * @throws IllegalArgumentException if callback is null
     * @throws IllegalStateException if audio permissions are not granted
     */
    void startListening(@NonNull SoundTransmissionCallback callback);

    /**
     * Stops listening for sound transmissions.
     * This operation is idempotent and safe to call multiple times.
     */
    void stopListening();

    /**
     * Sends data via sound transmission.
     * This is a non-blocking operation.
     * 
     * @param data The data to transmit. Must not be null or empty.
     * @param callback Optional callback to handle transmission status.
     * @throws IllegalArgumentException if data is null or empty
     * @throws IllegalStateException if not properly initialized
     */
    void sendData(@NonNull String data, @Nullable SoundTransmissionCallback callback);

    /**
     * Sends data via sound transmission without status callback.
     * 
     * @param data The data to transmit. Must not be null or empty.
     * @throws IllegalArgumentException if data is null or empty
     * @throws IllegalStateException if not properly initialized
     */
    void sendData(@NonNull String data);

    /**
     * Checks if currently listening for sound transmissions.
     * 
     * @return true if listening, false otherwise
     */
    boolean isListening();

    /**
     * Checks if sound transmission is properly initialized and ready for use.
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
     * Sets the volume level for sound transmissions.
     * 
     * @param volume Volume level between 0.0 and 1.0
     * @throws IllegalArgumentException if volume is outside valid range
     */
    void setTransmissionVolume(float volume);

    /**
     * Cleans up resources and stops all operations.
     * Should be called when sound transmission is no longer needed.
     */
    void cleanup();

    /**
     * Callback interface for sound transmission operations.
     * All methods are called on background threads.
     */
    interface SoundTransmissionCallback {
        /**
         * Called when data is successfully received via sound transmission.
         * 
         * @param data The received data. Never null.
         */
        void onDataReceived(@NonNull String data);

        /**
         * Called when data is successfully sent via sound transmission.
         * 
         * @param data The data that was sent. Never null.
         */
        void onDataSent(@NonNull String data);

        /**
         * Called when an error occurs during sound transmission operations.
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
