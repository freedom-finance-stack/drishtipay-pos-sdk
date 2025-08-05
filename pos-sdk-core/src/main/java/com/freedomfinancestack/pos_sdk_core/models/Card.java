package com.freedomfinancestack.pos_sdk_core.models;

import com.freedomfinancestack.pos_sdk_core.enums.CardType;
import com.freedomfinancestack.pos_sdk_core.enums.Network;
import com.freedomfinancestack.pos_sdk_core.enums.IssuerBank;
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