package com.freedomfinancestack.pos_sdk_core.constants;

import com.freedomfinancestack.pos_sdk_core.enums.GGWaveProtocol;

/**
 * Configuration constants for sound data transmission.
 * Internal use only - not part of public API.
 */
final class GGWaveConstants {
    
    // Public constants that library users might need
    public static final int MAX_DATA_LENGTH = 140;
    
    // Internal configuration - package private
    static final int SAMPLE_RATE = 48000;
    static final int SAMPLES_PER_FRAME = 1024;
    static final float DEFAULT_VOLUME = 0.5f;
    static final GGWaveProtocol DEFAULT_TX_PROTOCOL = GGWaveProtocol.DEFAULT_POS_PROTOCOL;
    static final GGWaveProtocol DEFAULT_RX_PROTOCOL = GGWaveProtocol.DEFAULT_LISTEN_PROTOCOL;
    static final String THREAD_NAME_PREFIX = "GGWave-Worker";
    static final long AUDIO_CAPTURE_DELAY_MS = 50;
    static final int LOG_DATA_TRUNCATE_LENGTH = 20;
    
    // Private constructor to prevent instantiation
    private GGWaveConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
}