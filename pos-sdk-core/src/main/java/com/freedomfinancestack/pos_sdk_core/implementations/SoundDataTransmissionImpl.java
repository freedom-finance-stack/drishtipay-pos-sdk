package com.freedomfinancestack.pos_sdk_core.implementations;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freedomfinancestack.pos_sdk_core.interfaces.ISoundDataTransmission;
import com.freedomfinancestack.pos_sdk_core.constants.GGWaveConstants;

import java.nio.ShortBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sound-based data transmission using GGWave technology.
 * Optimized for POS systems with inaudible ultrasound communication.
 */
public class SoundDataTransmissionImpl implements ISoundDataTransmission {
    
    private static final String TAG = "SoundDataTransmission";
    
    private final Context context;
    private final ExecutorService executor;
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    
    // Native library instance
    private long nativeInstance = 0;
    
    // Current callback reference
    private SoundCallback currentCallback;
    
    // Audio capture and playback
    private CapturingThread capturingThread;
    private PlaybackThread playbackThread;
    
    /**
     * Create new sound transmission instance.
     * @param context Android context for audio access
     */
    public SoundDataTransmissionImpl(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, GGWaveConstants.THREAD_NAME_PREFIX);
            t.setDaemon(true);
            return t;
        });
        
        initialize();
    }

    @Override
    public void listen(@NonNull SoundCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        
        if (isListening.compareAndSet(false, true)) {
            currentCallback = callback;
            
            // Initialize audio capture thread
            capturingThread = new CapturingThread(new AudioDataReceivedListener() {
                @Override
                public void onAudioDataReceived(short[] data) {
                    try {
                        // Process captured audio data through GGWave
                        String decodedMessage = processCaptureDataNative(nativeInstance, data);
                        if (decodedMessage != null && !decodedMessage.trim().isEmpty()) {
                            Log.d(TAG, "Received message: " + truncateForLog(decodedMessage));
                            if (currentCallback != null) {
                                currentCallback.onReceived(decodedMessage.trim());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing audio data", e);
                        if (currentCallback != null) {
                            currentCallback.onError("Audio processing error: " + e.getMessage());
                        }
                    }
                }
            });
            
            // Start audio capture
            try {
                capturingThread.startCapturing();
                Log.d(TAG, "Started listening for sound data");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start audio capture", e);
                isListening.set(false);
                if (currentCallback != null) {
                    currentCallback.onError("Failed to start audio capture: " + e.getMessage());
                }
            }
        } else {
            Log.w(TAG, "Already listening - ignoring duplicate request");
        }
    }

    @Override
    public void send(@NonNull String data, @Nullable SoundCallback callback) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        
        if (data.length() > GGWaveConstants.MAX_DATA_LENGTH) {
            throw new IllegalArgumentException("Data too long (max " + GGWaveConstants.MAX_DATA_LENGTH + " chars)");
        }
        
        executor.execute(() -> {
            try {
                if (nativeInstance == 0) {
                    Log.e(TAG, "Native instance not initialized: " + nativeInstance);
                    if (callback != null) {
                        callback.onError("Native instance not initialized");
                    }
                    return;
                }
                
                Log.d(TAG, "Sending data: " + truncateForLog(data));
                
                // Encode message to audio waveform
                short[] audioSamples = sendMessageNative(nativeInstance, data);
                
                if (audioSamples != null && audioSamples.length > 0) {
                    Log.d(TAG, "Message encoded to " + audioSamples.length + " audio samples");
                    
                    // Play the encoded audio
                    playbackThread = new PlaybackThread(audioSamples, new PlaybackListener() {
                        @Override
                        public void onProgress(int progress) {
                            // Progress updates can be added here if needed
                        }
                        
                        @Override
                        public void onCompletion() {
                            Log.d(TAG, "Audio playback completed");
                            if (callback != null) {
                                callback.onSent(data);
                            }
                            playbackThread = null;
                        }
                    });
                    
                    playbackThread.startPlayback();
                    Log.d(TAG, "Started playing encoded message");
                } else {
                    String error = "Failed to encode data to audio";
                    Log.e(TAG, error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            } catch (Exception e) {
                String error = "Send failed: " + e.getMessage();
                Log.e(TAG, error, e);
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    @Override
    public void send(@NonNull String data) {
        send(data, null);
    }

    @Override
    public void stop() {
        if (isListening.compareAndSet(true, false)) {
            try {
                // Stop audio capture
                if (capturingThread != null) {
                    capturingThread.stopCapturing();
                    capturingThread = null;
                }
                
                currentCallback = null;
                Log.d(TAG, "Stopped listening");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping listening", e);
            }
        }
        
        // Stop audio playback
        if (playbackThread != null) {
            playbackThread.stopPlayback();
            playbackThread = null;
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        
        if (nativeInstance != 0) {
            cleanupNative(nativeInstance);
            nativeInstance = 0;
        }
    }
    
    // Private methods
    
    private void initialize() {
        try {
            nativeInstance = initializeNative(
                GGWaveConstants.SAMPLE_RATE, 
                GGWaveConstants.SAMPLES_PER_FRAME
            );
            
            if (nativeInstance < 0) {
                throw new RuntimeException("Native initialization returned invalid instance: " + nativeInstance);
            }
            
            Log.d(TAG, "GGWave initialized successfully with instance: " + nativeInstance);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GGWave", e);
            throw new RuntimeException("GGWave initialization failed", e);
        }
    }
    

    
    private String truncateForLog(String data) {
        if (data == null) return "null";
        return data.length() > GGWaveConstants.LOG_DATA_TRUNCATE_LENGTH 
            ? data.substring(0, GGWaveConstants.LOG_DATA_TRUNCATE_LENGTH) + "..."
            : data;
    }
    
    // Native method declarations - JNI calls to GGWave library  
    private native long initializeNative(int sampleRate, int samplesPerFrame);
    private native String processCaptureDataNative(long instance, short[] audioData);
    private native short[] sendMessageNative(long instance, String message);
    private native void cleanupNative(long instance);
    
    static {
        try {
            // Load the GGWave native library
            System.loadLibrary("pos_sdk_ggwave");
            Log.d(TAG, "GGWave native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load GGWave native library", e);
            throw e;
        }
    }
}

/**
 * Interface for audio data received events
 */
interface AudioDataReceivedListener {
    void onAudioDataReceived(short[] data);
}

/**
 * Thread for capturing audio data from microphone
 */
class CapturingThread {
    private static final String LOG_TAG = "CapturingThread";
    private static final int SAMPLE_RATE = GGWaveConstants.SAMPLE_RATE;

    private boolean mShouldContinue;
    private AudioDataReceivedListener mListener;
    private Thread mThread;

    public CapturingThread(AudioDataReceivedListener listener) {
        mListener = listener;
    }

    public boolean capturing() {
        return mThread != null;
    }

    public void startCapturing() {
        if (mThread != null)
            return;

        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                capture();
            }
        });
        mThread.start();
    }

    public void stopCapturing() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;
    }

    private void capture() {
        Log.v(LOG_TAG, "Start");
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        bufferSize = GGWaveConstants.SAMPLES_PER_FRAME * 2;

        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        Log.d(LOG_TAG, "buffer size = " + bufferSize);
        Log.d(LOG_TAG, "Sample rate = " + record.getSampleRate());

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        Log.v(LOG_TAG, "Start capturing");

        long shortsRead = 0;
        while (mShouldContinue) {
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            shortsRead += numberOfShort;

            if (mListener != null) {
                mListener.onAudioDataReceived(audioBuffer);
            }
        }

        record.stop();
        record.release();

        Log.v(LOG_TAG, String.format("Capturing stopped. Samples read: %d", shortsRead));
    }
}

/**
 * Interface for playback completion events
 */
interface PlaybackListener {
    void onProgress(int progress);
    void onCompletion();
}

/**
 * Thread for playing audio data through speakers
 */
class PlaybackThread {
    static final int SAMPLE_RATE = GGWaveConstants.SAMPLE_RATE;
    private static final String LOG_TAG = "PlaybackThread";

    private Thread mThread;
    private boolean mShouldContinue;
    private ShortBuffer mSamples;
    private int mNumSamples;
    private PlaybackListener mListener;

    public PlaybackThread(short[] samples, PlaybackListener listener) {
        mSamples = ShortBuffer.wrap(samples);
        mNumSamples = samples.length;
        mListener = listener;
    }

    public boolean playing() {
        return mThread != null;
    }

    public void startPlayback() {
        if (mThread != null)
            return;

        // Start streaming in a thread
        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                play();
            }
        });
        mThread.start();
    }

    public void stopPlayback() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;
    }

    private void play() {
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        bufferSize = 16*1024;

        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);

        audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onPeriodicNotification(AudioTrack track) {
                if (mListener != null && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    mListener.onProgress((track.getPlaybackHeadPosition() * 1000) / SAMPLE_RATE);
                }
            }
            @Override
            public void onMarkerReached(AudioTrack track) {
                Log.v(LOG_TAG, "Audio file end reached");
                track.release();
                if (mListener != null) {
                    mListener.onCompletion();
                }
            }
        });
        audioTrack.setPositionNotificationPeriod(SAMPLE_RATE / 30); // 30 times per second
        audioTrack.setNotificationMarkerPosition(mNumSamples);

        audioTrack.play();

        Log.v(LOG_TAG, "Audio streaming started");

        short[] buffer = new short[bufferSize];
        mSamples.rewind();
        int limit = mNumSamples;
        int totalWritten = 0;
        while (mSamples.position() < limit && mShouldContinue) {
            int numSamplesLeft = limit - mSamples.position();
            int samplesToWrite;
            if (numSamplesLeft >= buffer.length) {
                mSamples.get(buffer);
                samplesToWrite = buffer.length;
            } else {
                for(int i = numSamplesLeft; i < buffer.length; i++) {
                    buffer[i] = 0;
                }
                mSamples.get(buffer, 0, numSamplesLeft);
                samplesToWrite = numSamplesLeft;
            }
            totalWritten += samplesToWrite;
            audioTrack.write(buffer, 0, samplesToWrite);
        }

        if (!mShouldContinue) {
            audioTrack.release();
        }

        Log.v(LOG_TAG, "Audio streaming finished. Samples written: " + totalWritten);
    }
}