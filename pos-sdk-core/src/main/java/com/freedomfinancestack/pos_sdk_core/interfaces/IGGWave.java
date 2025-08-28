package com.freedomfinancestack.pos_sdk_core.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.freedomfinancestack.pos_sdk_core.models.GGWaveMessage;

/**
 * Interface for GGWave audio data transmission functionality.
 * Enables sending and receiving data over sound waves using the GGWave protocol.
 * 
 * Threading: All callbacks are invoked on the main thread unless otherwise specified.
 * Lifecycle: Must call initialize() before using send/receive methods.
 * Error handling: Throws IllegalStateException for usage before initialization.
 */
public interface IGGWave {

    /**
     * Callback interface for receiving decoded messages from audio.
     */
    interface GGWaveCallback {
        /**
         * Called when a valid DrishtiPay message is successfully decoded from audio.
         * @param message The decoded and parsed GGWaveMessage, never null
         * @return true to continue listening, false to stop recording
         */
        boolean onMessageReceived(@NonNull GGWaveMessage message);
        
        /**
         * Called when raw audio data is decoded but doesn't match DrishtiPay format.
         * @param rawMessage The raw decoded text message
         * @return true to continue listening, false to stop recording
         */
        boolean onRawMessageReceived(@NonNull String rawMessage);
        
        /**
         * Called when an error occurs during reception or message parsing.
         * @param error Error message describing the failure
         */
        void onError(@NonNull String error);
    }

    /**
     * Callback interface for transmission events.
     */
    interface GGWaveTransmissionCallback {
        /**
         * Called when audio transmission is completed.
         */
        void onTransmissionComplete();
        
        /**
         * Called when transmission fails.
         * @param error Error message describing the failure
         */
        void onTransmissionError(@NonNull String error);
    }

    /**
     * Initialize the GGWave functionality.
     * Must be called before using send/receive methods.
     * Safe to call multiple times.
     * 
     * @param readyCallback Called when initialization is complete, nullable
     * @throws IllegalStateException if context is invalid
     */
    void initialize(@Nullable Runnable readyCallback);

    /**
     * Send a text message over audio waves.
     * 
     * @param message The text to transmit, must not be null or empty
     * @param useUltrasound true to use near-ultrasound frequencies, false for audible range
     * @param fastMode true for faster transmission with higher error risk
     * @param callback Optional callback for transmission events, nullable
     * @return true if transmission started successfully, false otherwise
     * @throws IllegalStateException if not initialized
     * @throws IllegalArgumentException if message is null or empty
     */
    boolean send(@NonNull String message, boolean useUltrasound, boolean fastMode, @Nullable GGWaveTransmissionCallback callback);

    /**
     * Send a text message over audio waves with default settings (audible, fast mode).
     * 
     * @param message The text to transmit, must not be null or empty
     * @return true if transmission started successfully, false otherwise
     * @throws IllegalStateException if not initialized
     * @throws IllegalArgumentException if message is null or empty
     */
    boolean send(@NonNull String message);

    /**
     * Send a structured DrishtiPay message over audio waves.
     * 
     * @param message The GGWaveMessage to transmit, must not be null
     * @param useUltrasound true to use near-ultrasound frequencies, false for audible range
     * @param fastMode true for faster transmission with higher error risk
     * @param callback Optional callback for transmission events, nullable
     * @return true if transmission started successfully, false otherwise
     * @throws IllegalStateException if not initialized
     * @throws IllegalArgumentException if message is null
     */
    boolean sendMessage(@NonNull GGWaveMessage message, boolean useUltrasound, boolean fastMode, @Nullable GGWaveTransmissionCallback callback);

    /**
     * Send a structured DrishtiPay message over audio waves with default settings.
     * 
     * @param message The GGWaveMessage to transmit, must not be null
     * @return true if transmission started successfully, false otherwise
     * @throws IllegalStateException if not initialized
     * @throws IllegalArgumentException if message is null
     */
    boolean sendMessage(@NonNull GGWaveMessage message);

    /**
     * Send a DrishtiPay message with mobile number using default settings.
     * 
     * @param mobileNumber The mobile number to transmit, must not be null or empty
     * @return true if transmission started successfully, false otherwise
     * @throws IllegalStateException if not initialized
     * @throws IllegalArgumentException if mobile number is null or empty
     */
    boolean sendMobileNumber(@NonNull String mobileNumber);

    /**
     * Start listening for incoming audio messages.
     * Non-blocking operation. Callbacks are invoked on main thread.
     * Safe to call multiple times.
     * 
     * @param callback Callback for received messages and errors, must not be null
     * @return true if listening started successfully, false otherwise
     * @throws IllegalStateException if not initialized
     * @throws IllegalArgumentException if callback is null
     */
    boolean startListening(@NonNull GGWaveCallback callback);

    /**
     * Stop listening for incoming audio messages.
     * Idempotent operation, safe to call multiple times.
     * Cancels any active recording and frees audio resources.
     */
    void stopListening();

    /**
     * Check if currently listening for messages.
     * @return true if actively listening, false otherwise
     */
    boolean isListening();

    /**
     * Check if GGWave is initialized and ready to use.
     * @return true if initialized, false otherwise
     */
    boolean isInitialized();

    /**
     * Clean up resources and stop all operations.
     * Should be called when no longer needed.
     * After calling cleanup(), initialize() must be called again before use.
     */
    void cleanup();
}
