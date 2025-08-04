package com.drishtipay.pos.models;

import com.drishtipay.pos.enums.CardType;
import com.drishtipay.pos.enums.Network;
import com.drishtipay.pos.enums.IssuerBank;
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



