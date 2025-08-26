package com.freedomfinancestack.pos_sdk_core.implementations;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebMessage;
import android.webkit.WebMessagePort;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freedomfinancestack.pos_sdk_core.interfaces.IGGWave;

import org.json.JSONObject;

/**
 * Implementation of IGGWave using WebView and GGWave JavaScript library.
 * 
 * This class wraps the GGWave JavaScript implementation in a WebView to provide
 * audio-based data transmission capabilities for Android applications.
 * 
 * Threading: All callbacks are executed on the main thread.
 * Permissions: Requires RECORD_AUDIO permission for receiving messages.
 * Resources: Call cleanup() when done to free WebView and audio resources.
 */
public class GGWaveManager implements IGGWave {
    
    private static final String TAG = "GGWaveManager";
    private static final String DEFAULT_HTML_LOCATION = "file:///android_asset/ggwave.html";
    private static final int AUDIO_STREAM = AudioManager.STREAM_MUSIC;
    
    private final Context context;
    private final String htmlLocation;
    private final boolean autoAdjustVolume;
    private final Handler mainHandler;
    private final AudioManager audioManager;
    
    private WebView webView;
    private WebMessagePort[] messageChannel;
    private GGWaveCallback currentCallback;
    private GGWaveTransmissionCallback currentTransmissionCallback;
    private boolean isInitialized = false;
    private boolean isListening = false;
    private int lastVolume;
    
    /**
     * Creates a new GGWaveManager instance.
     * 
     * @param context Application context, must not be null
     * @param autoAdjustVolume Whether to automatically adjust volume during transmission
     */
    public GGWaveManager(@NonNull Context context, boolean autoAdjustVolume) {
        this(context, DEFAULT_HTML_LOCATION, autoAdjustVolume);
    }
    
    /**
     * Creates a new GGWaveManager instance with custom HTML location.
     * 
     * @param context Application context, must not be null
     * @param htmlLocation Custom location for ggwave.html file
     * @param autoAdjustVolume Whether to automatically adjust volume during transmission
     */
    public GGWaveManager(@NonNull Context context, @NonNull String htmlLocation, boolean autoAdjustVolume) {
        this.context = context.getApplicationContext();
        this.htmlLocation = htmlLocation;
        this.autoAdjustVolume = autoAdjustVolume;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
    
    @Override
    public void initialize(@Nullable Runnable readyCallback) {
        if (!hasAudioPermission()) {
            throw new IllegalStateException("RECORD_AUDIO permission is required for GGWave functionality");
        }
        
        if (isInitialized) {
            if (readyCallback != null) {
                mainHandler.post(readyCallback);
            }
            return;
        }
        
        mainHandler.post(() -> initializeWebView(readyCallback));
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void initializeWebView(@Nullable Runnable readyCallback) {
        try {
            webView = new WebView(context);
            webView.getSettings().setJavaScriptEnabled(true);
            
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onPermissionRequest(PermissionRequest request) {
                    String[] resources = request.getResources();
                    if (resources != null) {
                        for (String resource : resources) {
                            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                                request.grant(new String[]{resource});
                                break;
                            }
                        }
                    }
                }
            });
            
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    initializeMessageChannel();
                    isInitialized = true;
                    Log.d(TAG, "GGWave initialized successfully");
                    
                    if (readyCallback != null) {
                        readyCallback.run();
                    }
                }
                
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Log.e(TAG, "WebView error: " + description);
                    if (currentCallback != null) {
                        currentCallback.onError("WebView initialization failed: " + description);
                    }
                }
            });
            
            webView.loadUrl(htmlLocation);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize WebView", e);
            throw new IllegalStateException("Failed to initialize GGWave WebView", e);
        }
    }
    
    private void initializeMessageChannel() {
        messageChannel = webView.createWebMessageChannel();
        
        messageChannel[0].setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
            @Override
            public void onMessage(WebMessagePort port, WebMessage message) {
                String data = message.getData();
                if (data == null) return;
                
                if (data.startsWith("message:")) {
                    handleReceivedMessage(data.substring(8));
                } else if ("onTxEnded".equals(data)) {
                    handleTransmissionComplete();
                }
            }
        });
        
        webView.postWebMessage(new WebMessage("port", new WebMessagePort[]{messageChannel[1]}), Uri.EMPTY);
    }
    
    private void handleReceivedMessage(@NonNull String message) {
        Log.d(TAG, "Received message: [REDACTED]"); // Don't log actual message for privacy
        
        if (currentCallback != null) {
            try {
                boolean shouldContinue = currentCallback.onMessageReceived(message);
                if (!shouldContinue) {
                    stopListening();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in message callback", e);
                currentCallback.onError("Callback error: " + e.getMessage());
            }
        }
    }
    
    private void handleTransmissionComplete() {
        Log.d(TAG, "Transmission completed");
        
        if (autoAdjustVolume) {
            audioManager.setStreamVolume(AUDIO_STREAM, lastVolume, 0);
        }
        
        if (currentTransmissionCallback != null) {
            try {
                currentTransmissionCallback.onTransmissionComplete();
            } catch (Exception e) {
                Log.e(TAG, "Error in transmission callback", e);
            }
            currentTransmissionCallback = null;
        }
    }
    
    @Override
    public boolean send(@NonNull String message, boolean useUltrasound, boolean fastMode, @Nullable GGWaveTransmissionCallback callback) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        
        if (!isInitialized) {
            throw new IllegalStateException("GGWave not initialized. Call initialize() first.");
        }
        
        try {
            JSONObject command = new JSONObject();
            command.put("message", message.trim());
            command.put("useUltrasound", useUltrasound);
            command.put("fastMode", fastMode);
            
            currentTransmissionCallback = callback;
            
            if (autoAdjustVolume) {
                lastVolume = audioManager.getStreamVolume(AUDIO_STREAM);
                audioManager.setStreamVolume(AUDIO_STREAM, audioManager.getStreamMaxVolume(AUDIO_STREAM), 0);
            }
            
            WebMessage webMessage = new WebMessage("send:" + command.toString());
            webView.postWebMessage(webMessage, Uri.EMPTY);
            
            Log.d(TAG, "Message transmission started");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send message", e);
            if (callback != null) {
                callback.onTransmissionError("Failed to send: " + e.getMessage());
            }
            return false;
        }
    }
    
    @Override
    public boolean send(@NonNull String message) {
        return send(message, false, true, null);
    }
    
    @Override
    public boolean startListening(@NonNull GGWaveCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        
        if (!isInitialized) {
            throw new IllegalStateException("GGWave not initialized. Call initialize() first.");
        }
        
        if (!hasAudioPermission()) {
            callback.onError("RECORD_AUDIO permission is required for listening");
            return false;
        }
        
        try {
            currentCallback = callback;
            isListening = true;
            
            WebMessage webMessage = new WebMessage("startRecording");
            webView.postWebMessage(webMessage, Uri.EMPTY);
            
            Log.d(TAG, "Started listening for messages");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start listening", e);
            callback.onError("Failed to start listening: " + e.getMessage());
            isListening = false;
            currentCallback = null;
            return false;
        }
    }
    
    @Override
    public void stopListening() {
        if (!isListening) {
            return;
        }
        
        try {
            isListening = false;
            currentCallback = null;
            
            if (webView != null) {
                WebMessage webMessage = new WebMessage("stopRecording");
                webView.postWebMessage(webMessage, Uri.EMPTY);
            }
            
            Log.d(TAG, "Stopped listening for messages");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
        }
    }
    
    @Override
    public boolean isListening() {
        return isListening;
    }
    
    @Override
    public boolean isInitialized() {
        return isInitialized;
    }
    
    @Override
    public void cleanup() {
        Log.d(TAG, "Cleaning up GGWave resources");
        
        stopListening();
        currentTransmissionCallback = null;
        
        if (webView != null) {
            try {
                webView.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destroying WebView", e);
            }
            webView = null;
        }
        
        messageChannel = null;
        isInitialized = false;
    }
    
    private boolean hasAudioPermission() {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
}