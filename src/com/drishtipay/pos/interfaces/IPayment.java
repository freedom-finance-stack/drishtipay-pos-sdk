package com.drishtipay.pos.interfaces;


import com.drishtipay.pos.enums.PaymentStatus;
import com.drishtipay.pos.models.Card;
import com.drishtipay.pos.models.PaymentInitiationResponse;

public interface IPayment {
    // initiate payment with selected saved card and return the acs url for entering issuer sent OTP.
    PaymentInitiationResponse initiatePayment(Card card, float amount);

    // Confirm Payment status for given Payment ID.
    PaymentStatus confirmPayment(String paymentId);
}
