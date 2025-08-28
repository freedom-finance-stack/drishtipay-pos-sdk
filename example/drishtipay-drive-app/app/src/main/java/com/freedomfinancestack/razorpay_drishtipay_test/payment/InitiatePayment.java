package com.freedomfinancestack.razorpay_drishtipay_test.payment;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.freedomfinancestack.pos_sdk_core.enums.PaymentStatus;
import com.freedomfinancestack.pos_sdk_core.interfaces.IPayment;
import com.freedomfinancestack.pos_sdk_core.models.Card;
import com.freedomfinancestack.pos_sdk_core.models.PaymentInitiationResponse;
import com.freedomfinancestack.razorpay_drishtipay_test.BuildConfig;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

public class InitiatePayment implements IPayment {

    private static final String TAG = "InitiatePayment";
    private Context context;
    private String RzpKey;
    private String RzpSecret;
    
    public InitiatePayment(Context context) {
        this.context = context;
        
        // Initialize credentials from BuildConfig
        this.RzpKey = BuildConfig.RAZORPAY_KEY;
        this.RzpSecret = BuildConfig.RAZORPAY_SECRET;
        
        // Debug logging to verify keys are loaded
        if (BuildConfig.DEBUG_MODE) {
            Log.d(TAG, "Razorpay Key loaded: " + (RzpKey.isEmpty() ? "EMPTY" : RzpKey.substring(0, Math.min(8, RzpKey.length())) + "..."));
            Log.d(TAG, "Razorpay Secret loaded: " + (RzpSecret.isEmpty() ? "EMPTY" : "***"));
        }
    }

    @Override
    public PaymentInitiationResponse initiatePayment(Card card, float amount) {
        PaymentInitiationResponse response = new PaymentInitiationResponse("","","");
        
        try {
            // Validate inputs first
            if (amount <= 0) {
                showToast("Invalid amount: " + amount);
                throw new IllegalArgumentException("Amount must be greater than 0");
            }
            
            if (RzpKey == null || RzpKey.isEmpty() || RzpSecret == null || RzpSecret.isEmpty()) {
                showToast("Payment configuration error");
                throw new IllegalStateException("Razorpay API keys not configured");
            }
            
            showToast("Creating payment order...");
            
            // CURL 1: Create Order
            String orderId = createOrder(amount);
            
            showToast("Processing payment...");
            
            // CURL 2: Create Payment 
            String paymentResponse = createPayment(card, amount, orderId);
            
            // Put HTML response directly in acsURL
            response = new PaymentInitiationResponse(paymentResponse, "", orderId);
            
            showToast("Payment initiated successfully");
            Log.d(TAG, "Payment initiated successfully with order ID: " + orderId);

        } catch (Exception e) {
            String errorMsg = "Payment failed: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            showToast(errorMsg);
            
            // Return error response
            response = new PaymentInitiationResponse("", "ERROR: " + e.getMessage(), "");
        }
        
        return response;
    }

    private String createOrder(float amount) throws Exception {
        try {
            // Exact CURL from docs: curl -X POST https://api.razorpay.com/v1/orders
            JSONObject json = new JSONObject();
            json.put("amount", Math.round(amount * 100)); // Razorpay expects amount in paise
            json.put("currency", "INR");
            json.put("receipt", "receipt_" + System.currentTimeMillis());

            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
                
            RequestBody body = RequestBody.create(
                json.toString(), 
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(BuildConfig.BASE_URL + "orders")
                .post(body)
                .addHeader("Authorization", okhttp3.Credentials.basic(RzpKey, RzpSecret))
                .addHeader("Content-Type", "application/json")
                .build();

            Response response = client.newCall(request).execute();
            
            // Check if response body exists
            if (response.body() == null) {
                throw new Exception("Empty response from Razorpay API");
            }
            
            String responseBody = response.body().string();
            Log.d(TAG, "Order Response Code: " + response.code());
            
            // Check HTTP status
            if (!response.isSuccessful()) {
                Log.e(TAG, "Order API Error: " + responseBody);
                throw new Exception("Order creation failed (HTTP " + response.code() + ")");
            }
            
            // Parse JSON response
            JSONObject result = new JSONObject(responseBody);
            
            // Check if order ID exists in response
            if (!result.has("id")) {
                throw new Exception("Invalid response: missing order ID");
            }
            
            String orderId = result.getString("id");
            Log.d(TAG, "Order created successfully: " + orderId);
            return orderId;
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error in createOrder", e);
            throw new Exception("Failed to parse order response");
        } catch (Exception e) {
            Log.e(TAG, "Error creating order", e);
            throw new Exception("Order creation failed: " + e.getMessage());
        }
    }

    private String createPayment(Card card, float amount, String orderId) throws Exception {
        try {
            // Validate inputs
            if (orderId == null || orderId.isEmpty()) {
                throw new IllegalArgumentException("Order ID cannot be empty");
            }
            
            // Exact CURL from docs: curl -X POST https://api.razorpay.com/v1/payments
            JSONObject cardJson = new JSONObject();
            cardJson.put("number", "4386 2894 0766 0153");
            cardJson.put("name", "dummy");
            cardJson.put("expiry_month", 10);
            cardJson.put("expiry_year", 2028);
            cardJson.put("cvv", 123);

            JSONObject json = new JSONObject();
            json.put("amount", Math.round(amount * 100)); // Amount in paise
            json.put("currency", "INR");
            json.put("order_id", orderId);
            json.put("method", "card");
            json.put("card", cardJson);
            json.put("email", "test@example.com");
            json.put("contact", "9999999999");

            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
                
            RequestBody body = RequestBody.create(
                json.toString(), 
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(BuildConfig.BASE_URL + "payments")
                .post(body)
//                .addHeader("Authorization", okhttp3.Credentials.basic(RzpKey, RzpSecret))
                .addHeader("Content-Type", "application/json")
                .build();

            Response response = client.newCall(request).execute();
            
            // Check if response body exists
            if (response.body() == null) {
                throw new Exception("Empty response from Razorpay payments API");
            }
            
            String responseBody = response.body().string();
            Log.d(TAG, "Payment Response Code: " + response.code());
            
            // Log response for debugging (be careful with sensitive data in production)
            if (BuildConfig.DEBUG_MODE) {
                Log.d(TAG, "Payment Response: " + responseBody);
            }
            
            // Check HTTP status
            if (!response.isSuccessful()) {
                Log.e(TAG, "Payment API Error: " + responseBody);
                throw new Exception("Payment processing failed (HTTP " + response.code() + ")");
            }
            
            // Return raw response (HTML or JSON)
            return responseBody;
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error in createPayment", e);
            throw new Exception("Failed to create payment request");
        } catch (Exception e) {
            Log.e(TAG, "Error creating payment", e);
            throw new Exception("Payment processing failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentStatus confirmPayment(String s) {
        
        return null;
    }
    
    /**
     * Helper method to show toast messages on UI thread
     */
    private void showToast(String message) {
        if (context != null) {
            // Ensure toast is shown on UI thread
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            } else {
                // Post to main thread if called from background thread
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
            }
        }
        // Also log the message
        Log.i(TAG, "Toast: " + message);
    }


}