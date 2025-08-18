package com.freedomfinancestack.pos_sdk_core.constants;

/**
 * Constants for GGWave sound-based data transmission configuration.
 * 
 * This class contains all configuration constants used by GGWave implementations
 * to avoid hardcoding values throughout the codebase.
 */
public final class GGWaveConstants {
    
    // Audio Configuration
    public static final int SAMPLE_RATE = 48000;
    public static final int SAMPLES_PER_FRAME = 1024;
    public static final float DEFAULT_VOLUME = 0.5f;
    public static final int MAX_DATA_LENGTH = 140; // GGWave typical limit
    
    // Threading Configuration
    public static final String THREAD_NAME_PREFIX = "GGWave-Worker";
    public static final String SCHEDULER_THREAD_NAME = "GGWaveFlow-Scheduler";
    public static final long AUDIO_CAPTURE_DELAY_MS = 50;
    
    // Protocol Constants
    public static final String PAIRING_REQUEST_PREFIX = "PAIR_REQ:";
    public static final String PAIRING_RESPONSE_PREFIX = "PAIR_RSP:";
    public static final String DATA_TRANSFER_PREFIX = "DATA:";
    public static final String DATA_REQUEST_PREFIX = "REQ:";
    public static final String ACK_PREFIX = "ACK:";
    
    // Timeout Configuration
    public static final long DEFAULT_PAIRING_TIMEOUT_MS = 30000; // 30 seconds
    
    // Audio Stream Configuration
    public static final int MOCK_AUDIO_DURATION_SECONDS = 2;
    
    // Logging Configuration
    public static final int LOG_DATA_TRUNCATE_LENGTH = 20;
    
    // Volume Range Validation
    public static final float MIN_VOLUME = 0.0f;
    public static final float MAX_VOLUME = 1.0f;
    
    // Private constructor to prevent instantiation
    private GGWaveConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
}