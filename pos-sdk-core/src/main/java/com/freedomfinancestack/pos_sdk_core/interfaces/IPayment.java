package com.freedomfinancestack.pos_sdk_core.interfaces;


import com.freedomfinancestack.pos_sdk_core.enums.PaymentStatus;
import com.freedomfinancestack.pos_sdk_core.models.Card;
import com.freedomfinancestack.pos_sdk_core.models.PaymentInitiationResponse;

public interface IPayment {
    // initiate payment with selected saved card and return the acs url for entering issuer sent OTP.
    PaymentInitiationResponse initiatePayment(Card card, float amount);

    // Confirm Payment status for given Payment ID.
    PaymentStatus confirmPayment(String paymentId);
} 