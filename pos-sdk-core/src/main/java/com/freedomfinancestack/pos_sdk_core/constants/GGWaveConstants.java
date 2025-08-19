package com.freedomfinancestack.pos_sdk_core.constants;

/**
 * Configuration constants for sound data transmission.
 * Package-private - internal use only.
 */
public class GGWaveConstants {
    
    // Public constants that library users might need
    public static final int MAX_DATA_LENGTH = 140;
    
    // Internal configuration - accessible to implementations but not exposed in docs
    public static final int SAMPLE_RATE = 48000;
    public static final int SAMPLES_PER_FRAME = 1024;
    public static final float DEFAULT_VOLUME = 0.5f;
    // Use AUDIBLE_FAST protocol (ID=1) like official ggwave-java reference
    public static final int DEFAULT_TX_PROTOCOL_ID = 1; // GGWAVE_TX_PROTOCOL_AUDIBLE_FAST 
    public static final int DEFAULT_RX_PROTOCOL_ID = 1; // GGWAVE_TX_PROTOCOL_AUDIBLE_FAST
    public static final String THREAD_NAME_PREFIX = "GGWave-Worker";
    public static final long AUDIO_CAPTURE_DELAY_MS = 50;
    public static final int LOG_DATA_TRUNCATE_LENGTH = 20;
    
    // Private constructor to prevent instantiation
    private GGWaveConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
}