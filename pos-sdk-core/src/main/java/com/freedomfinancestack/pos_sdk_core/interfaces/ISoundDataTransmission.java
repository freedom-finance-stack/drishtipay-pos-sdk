package com.freedomfinancestack.pos_sdk_core.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Simplified interface for sound-based data transmission using GGWave technology.
 * 
 * Core functionality for transmitting and receiving data through inaudible sound waves.
 * Uses optimized ultrasound protocol by default for fast, silent data exchange.
 * 
 * USAGE:
 * 1. Ensure RECORD_AUDIO permission is granted
 * 2. Call listen() to start receiving data
 * 3. Call send() to transmit data  
 * 4. Call stop() when done
 * 
 * THREADING: All callbacks execute on background threads.
 */
public interface ISoundDataTransmission {

    /**
     * Start listening for incoming data transmissions.
     * Non-blocking operation with callback-based results.
     * 
     * @param callback Handles received data and errors
     * @throws IllegalArgumentException if callback is null
     * @throws IllegalStateException if audio permissions missing
     */
    void listen(@NonNull SoundCallback callback);

    /**
     * Send data via sound transmission.
     * Non-blocking operation with optional status callback.
     * 
     * @param data Data to transmit (max 140 characters)
     * @param callback Optional transmission status callback
     * @throws IllegalArgumentException if data is null/empty/too long
     */
    void send(@NonNull String data, @Nullable SoundCallback callback);

    /**
     * Send data via sound transmission.
     * Fire-and-forget operation without status callback.
     * 
     * @param data Data to transmit (max 140 characters)
     * @throws IllegalArgumentException if data is null/empty/too long
     */
    void send(@NonNull String data);

    /**
     * Stop all sound operations and release resources.
     * Safe to call multiple times.
     */
    void stop();

    /**
     * Simplified callback for sound operations.
     * All methods execute on background threads.
     */
    interface SoundCallback {
        /**
         * Data successfully received.
         * @param data Received data
         */
        void onReceived(@NonNull String data);

        /**
         * Data successfully sent.
         * @param data Sent data
         */
        void onSent(@NonNull String data);

        /**
         * Operation failed.
         * @param error Error description
         */
        void onError(@NonNull String error);
    }
}
