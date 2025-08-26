package com.freedomfinancestack.pos_sdk_core.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model class representing a standardized GGWave message format.
 * Ensures consistent JSON structure for all GGWave transmissions.
 */
public class GGWaveMessage {
    
    // JSON field names
    private static final String FIELD_MOBILE_NO = "mobile_no";
    private static final String FIELD_APP_TYPE = "app_type";
    private static final String FIELD_TRANSMISSION_TYPE = "transmission_type";
    
    // Default values
    private static final String DEFAULT_APP_TYPE = "drishtipay_app";
    private static final String DEFAULT_TRANSMISSION_TYPE = "ggwave";
    
    private final String mobileNumber;
    private final String appType;
    private final String transmissionType;
    
    /**
     * Constructor with mobile number and default values.
     * @param mobileNumber The mobile number, must not be null or empty
     */
    public GGWaveMessage(@NonNull String mobileNumber) {
        this(mobileNumber, DEFAULT_APP_TYPE, DEFAULT_TRANSMISSION_TYPE);
    }
    
    /**
     * Full constructor with all parameters.
     * @param mobileNumber The mobile number, must not be null or empty
     * @param appType The app type identifier
     * @param transmissionType The transmission type identifier
     */
    public GGWaveMessage(@NonNull String mobileNumber, @NonNull String appType, @NonNull String transmissionType) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number cannot be null or empty");
        }
        if (appType == null || appType.trim().isEmpty()) {
            throw new IllegalArgumentException("App type cannot be null or empty");
        }
        if (transmissionType == null || transmissionType.trim().isEmpty()) {
            throw new IllegalArgumentException("Transmission type cannot be null or empty");
        }
        
        this.mobileNumber = mobileNumber.trim();
        this.appType = appType.trim();
        this.transmissionType = transmissionType.trim();
    }
    
    /**
     * Create GGWaveMessage from JSON string.
     * @param jsonString The JSON string to parse
     * @return GGWaveMessage instance
     * @throws IllegalArgumentException if JSON is invalid or missing required fields
     */
    @NonNull
    public static GGWaveMessage fromJson(@NonNull String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        
        try {
            JSONObject json = new JSONObject(jsonString.trim());
            
            // Validate required fields
            if (!json.has(FIELD_MOBILE_NO)) {
                throw new IllegalArgumentException("Missing required field: " + FIELD_MOBILE_NO);
            }
            
            String mobileNo = json.getString(FIELD_MOBILE_NO);
            String appType = json.optString(FIELD_APP_TYPE, DEFAULT_APP_TYPE);
            String transmissionType = json.optString(FIELD_TRANSMISSION_TYPE, DEFAULT_TRANSMISSION_TYPE);
            
            return new GGWaveMessage(mobileNo, appType, transmissionType);
            
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert to JSON string format.
     * @return JSON string representation
     */
    @NonNull
    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put(FIELD_MOBILE_NO, mobileNumber);
            json.put(FIELD_APP_TYPE, appType);
            json.put(FIELD_TRANSMISSION_TYPE, transmissionType);
            return json.toString();
        } catch (JSONException e) {
            // This should never happen with our controlled data
            throw new RuntimeException("Failed to create JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the mobile number.
     * @return The mobile number
     */
    @NonNull
    public String getMobileNumber() {
        return mobileNumber;
    }
    
    /**
     * Get the app type.
     * @return The app type
     */
    @NonNull
    public String getAppType() {
        return appType;
    }
    
    /**
     * Get the transmission type.
     * @return The transmission type
     */
    @NonNull
    public String getTransmissionType() {
        return transmissionType;
    }
    
    /**
     * Validate if this is a DrishtiPay GGWave message.
     * @return true if valid DrishtiPay message format
     */
    public boolean isValidDrishtiPayMessage() {
        return DEFAULT_APP_TYPE.equals(appType) && 
               DEFAULT_TRANSMISSION_TYPE.equals(transmissionType) &&
               isValidMobileNumber(mobileNumber);
    }
    
    /**
     * Basic mobile number validation.
     * @param mobileNumber The mobile number to validate
     * @return true if appears to be a valid mobile number
     */
    private boolean isValidMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.length() < 10) {
            return false;
        }
        // Basic check: only digits, length between 10-15
        return mobileNumber.matches("\\d{10,15}");
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        GGWaveMessage that = (GGWaveMessage) obj;
        return mobileNumber.equals(that.mobileNumber) &&
               appType.equals(that.appType) &&
               transmissionType.equals(that.transmissionType);
    }
    
    @Override
    public int hashCode() {
        int result = mobileNumber.hashCode();
        result = 31 * result + appType.hashCode();
        result = 31 * result + transmissionType.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return "GGWaveMessage{" +
                "mobileNumber='" + mobileNumber + '\'' +
                ", appType='" + appType + '\'' +
                ", transmissionType='" + transmissionType + '\'' +
                '}';
    }
}