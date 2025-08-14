package com.freedomfinancestack.razorpay_drishtipay_test.payment;

import android.util.Log;
import com.freedomfinancestack.pos_sdk_core.enums.PaymentStatus;
import com.freedomfinancestack.pos_sdk_core.interfaces.IPayment;
import com.freedomfinancestack.pos_sdk_core.models.Card;
import com.freedomfinancestack.pos_sdk_core.models.PaymentInitiationResponse;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InitiatePayment implements IPayment {

    String RzpKey = "???"; // put your RzpKey
    String RzpSecret = "???"; // put your RzpSecret

    @Override
    public PaymentInitiationResponse initiatePayment(Card card, float amount) {
        PaymentInitiationResponse response = new PaymentInitiationResponse("","","");
        
        try {
            // CURL 1: Create Order
            String orderId = createOrder(1000);
            
            // CURL 2: Create Payment 
            String paymentResponse = createPayment(card, 1000, orderId);
            
            // Put HTML response directly in acsURL
            response = new PaymentInitiationResponse(paymentResponse, "", orderId);

            Log.d("Response from the line 34", "Here is the response" + response);


        } catch (Exception e) {
            Log.e("Payment", "Failed: " + e.getMessage());
        }
        
        return response;
    }

    private String createOrder(float amount) throws Exception {
        // Exact CURL from docs: curl -X POST https://api.razorpay.com/v1/orders
        JSONObject json = new JSONObject();
        json.put("amount", Math.round(amount));
        json.put("currency", "INR");
        json.put("receipt", "receipt_" + System.currentTimeMillis());

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
            json.toString(), 
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url("https://api.razorpay.com/v1/orders")
            .post(body)
            .addHeader("Authorization", okhttp3.Credentials.basic(RzpKey, RzpSecret))
            .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        Log.d("RazorpayOrder", "Order Response: " + responseBody);
        Log.d("RazorpayOrder", "Response Code: " + response.code());
        
        JSONObject result = new JSONObject(responseBody);
        return result.getString("id");
    }

    private String createPayment(Card card, float amount, String orderId) throws Exception {
        // Exact CURL from docs: curl -X POST https://api.razorpay.com/v1/payments
        JSONObject cardJson = new JSONObject();
        cardJson.put("number", "4386 2894 0766 0153");
        cardJson.put("name", "dummy");
        cardJson.put("expiry_month", 10);
        cardJson.put("expiry_year", 2028);
        cardJson.put("cvv", 123);

        JSONObject json = new JSONObject();
        json.put("amount", Math.round(amount));
        json.put("currency", "INR");
        json.put("order_id", orderId);
        json.put("method", "card");
        json.put("card", cardJson);
        json.put("email", "test@example.com");
        json.put("contact", "9999999999");

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
            json.toString(), 
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url("https://api.razorpay.com/v1/payments")
            .post(body)
            // .addHeader("Authorization", okhttp3.Credentials.basic(RzpKey, RzpSecret))
            .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        Log.d("RazorpayPayment", "Payment Response: " + responseBody);
        Log.d("RazorpayPayment", "Response Code: " + response.code());
        
        // Return raw response (HTML or JSON)
        return responseBody;
    }

        @Override
    public PaymentStatus confirmPayment(String s) {
        
        return null;
    }

    // Test method - call this from your MainActivity
    public void testPayment() {
        // Create a dummy card for testing
        Card testCard = Card.builder()
            .cardId("test_card_123")
            .last4Digits("1234")
            .build();
        
        Log.d("PaymentTest", "Starting payment test...");
        
        PaymentInitiationResponse response = initiatePayment(testCard, 1000.0f);
        
        Log.d("PaymentTest", "Payment test completed");
        Log.d("PaymentTest", "Response: " + (response != null ? "Success" : "Failed"));
    }
}