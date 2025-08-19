package com.freedomfinancestack.pos_sdk_core.enums;

/**
 * Internal GGWave protocol configuration.
 * Package-private - not part of public API.
 */
enum GGWaveProtocol {
    
    // Audible Protocols (Human audible range: 1875-6375 Hz)
    GGWAVE_PROTOCOL_AUDIBLE_NORMAL(
        0, 
        "Audible Normal", 
        1875.0f, 
        46.875f, 
        6, 
        9, 
        false,
        "Standard audible protocol with good balance of speed and reliability"
    ),
    
    GGWAVE_PROTOCOL_AUDIBLE_FAST(
        1, 
        "Audible Fast", 
        1875.0f, 
        46.875f, 
        6, 
        6, 
        false,
        "Faster audible transmission with moderate reliability"
    ),
    
    GGWAVE_PROTOCOL_AUDIBLE_FASTEST(
        2, 
        "Audible Fastest", 
        1875.0f, 
        46.875f, 
        6, 
        3, 
        false,
        "Fastest audible transmission with lower reliability"
    ),
    
    // Ultrasound Protocols (Inaudible range: 15000-19500 Hz)
    GGWAVE_PROTOCOL_ULTRASOUND_NORMAL(
        3, 
        "Ultrasound Normal", 
        15000.0f, 
        46.875f, 
        6, 
        9, 
        true,
        "Standard ultrasound protocol - inaudible to humans with good reliability"
    ),
    
    GGWAVE_PROTOCOL_ULTRASOUND_FAST(
        4, 
        "Ultrasound Fast", 
        15000.0f, 
        46.875f, 
        6, 
        6, 
        true,
        "Faster ultrasound transmission with moderate reliability"
    ),
    
    GGWAVE_PROTOCOL_ULTRASOUND_FASTEST(
        5, 
        "Ultrasound Fastest", 
        15000.0f, 
        46.875f, 
        6, 
        3, 
        true,
        "Fastest ultrasound transmission - ideal for quick POS transactions"
    ),
    
    // Dual-Tone Protocols (Low frequency range: 1125-2625 Hz)
    GGWAVE_PROTOCOL_DUAL_TONE_NORMAL(
        6, 
        "Dual-Tone Normal", 
        1125.0f, 
        46.875f, 
        2, 
        9, 
        false,
        "Dual-tone protocol for compatibility with talking buttons and low-quality speakers"
    ),
    
    GGWAVE_PROTOCOL_DUAL_TONE_FAST(
        7, 
        "Dual-Tone Fast", 
        1125.0f, 
        46.875f, 
        2, 
        6, 
        false,
        "Faster dual-tone transmission"
    ),
    
    GGWAVE_PROTOCOL_DUAL_TONE_FASTEST(
        8, 
        "Dual-Tone Fastest", 
        1125.0f, 
        46.875f, 
        2, 
        3, 
        false,
        "Fastest dual-tone transmission"
    );
    
    /**
     * Default protocol for POS SDK operations.
     * Uses ultrasound fastest for discrete, quick transactions.
     */
    public static final GGWaveProtocol DEFAULT_POS_PROTOCOL = GGWAVE_PROTOCOL_ULTRASOUND_FASTEST;
    
    /**
     * Default protocol for listening operations.
     * Uses ultrasound fastest to match transmission protocol.
     */
    public static final GGWaveProtocol DEFAULT_LISTEN_PROTOCOL = GGWAVE_PROTOCOL_ULTRASOUND_FASTEST;
    
    private final int protocolId;
    private final String displayName;
    private final float baseFrequency;
    private final float frequencyStep;
    private final int toneCount;
    private final int framesPerTx;
    private final boolean isUltrasound;
    private final String description;
    
    GGWaveProtocol(int protocolId, String displayName, float baseFrequency, 
                   float frequencyStep, int toneCount, int framesPerTx, 
                   boolean isUltrasound, String description) {
        this.protocolId = protocolId;
        this.displayName = displayName;
        this.baseFrequency = baseFrequency;
        this.frequencyStep = frequencyStep;
        this.toneCount = toneCount;
        this.framesPerTx = framesPerTx;
        this.isUltrasound = isUltrasound;
        this.description = description;
    }
    
    /**
     * @return Unique protocol identifier for native library
     */
    public int getProtocolId() {
        return protocolId;
    }
    
    /**
     * @return Human-readable protocol name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * @return Base frequency in Hz for this protocol
     */
    public float getBaseFrequency() {
        return baseFrequency;
    }
    
    /**
     * @return Frequency step between tones in Hz
     */
    public float getFrequencyStep() {
        return frequencyStep;
    }
    
    /**
     * @return Number of simultaneous tones used for encoding
     */
    public int getToneCount() {
        return toneCount;
    }
    
    /**
     * @return Number of frames per transmission (affects speed vs reliability)
     */
    public int getFramesPerTx() {
        return framesPerTx;
    }
    
    /**
     * @return True if this protocol uses ultrasound frequencies (inaudible)
     */
    public boolean isUltrasound() {
        return isUltrasound;
    }
    
    /**
     * @return Maximum frequency used by this protocol in Hz
     */
    public float getMaxFrequency() {
        return baseFrequency + (toneCount * 16 * frequencyStep);
    }
    
    /**
     * @return Protocol description and use case information
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * @return True if this protocol is suitable for POS transactions (discrete and fast)
     */
    public boolean isSuitableForPOS() {
        return isUltrasound; // Ultrasound protocols are discrete
    }
    
    /**
     * Get protocol by ID
     * @param protocolId Protocol identifier
     * @return Matching protocol or null if not found
     */
    public static GGWaveProtocol fromId(int protocolId) {
        for (GGWaveProtocol protocol : values()) {
            if (protocol.getProtocolId() == protocolId) {
                return protocol;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%.0f-%.0f Hz, %d tones, %s)", 
            displayName, baseFrequency, getMaxFrequency(), toneCount,
            isUltrasound ? "ultrasound" : "audible");
    }
}