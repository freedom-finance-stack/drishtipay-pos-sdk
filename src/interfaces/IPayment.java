package interfaces;


import enums.PaymentStatus;
import models.Card;
import models.PaymentInitiationRequest;

public interface IPayment {
    // initiate payment with selected saved card and return the acs url for entering issuer sent OTP.
    PaymentInitiationRequest initiatePayment(Card card, float amt);

    // Confirm Payment status for given Payment ID.
    PaymentStatus confirmPayment(String PaymentID);
}
