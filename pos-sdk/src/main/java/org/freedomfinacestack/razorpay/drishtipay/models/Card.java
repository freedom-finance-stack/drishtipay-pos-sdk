package org.freedomfinacestack.razorpay.drishtipay.models;

import org.freedomfinacestack.razorpay.drishtipay.enums.CardType;
import org.freedomfinacestack.razorpay.drishtipay.enums.Network;
import org.freedomfinacestack.razorpay.drishtipay.enums.IssuerBank;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;


@Builder
@Data
@AllArgsConstructor
public class Card {
    private String cardId;
    private String last4Digits;
    private Network network;
    private CardType cardType ;
    private IssuerBank issuerBank ;
} 