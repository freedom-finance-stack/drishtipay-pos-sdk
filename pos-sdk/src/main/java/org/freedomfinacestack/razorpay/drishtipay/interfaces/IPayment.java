package org.freedomfinacestack.razorpay.drishtipay.interfaces;


import org.freedomfinacestack.razorpay.drishtipay.enums.PaymentStatus;
import org.freedomfinacestack.razorpay.drishtipay.models.Card;
import org.freedomfinacestack.razorpay.drishtipay.models.PaymentInitiationResponse;

public interface IPayment {
    // initiate payment with selected saved card and return the acs url for entering issuer sent OTP.
    PaymentInitiationResponse initiatePayment(Card card, float amount);

    // Confirm Payment status for given Payment ID.
    PaymentStatus confirmPayment(String paymentId);
} 