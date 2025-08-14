package com.freedomfinancestack.razorpay_drishtipay_test.savedcards;

import com.freedomfinancestack.pos_sdk_core.interfaces.ICards;
import com.freedomfinancestack.pos_sdk_core.models.Card;
import com.freedomfinancestack.pos_sdk_core.enums.CardType;
import com.freedomfinancestack.pos_sdk_core.enums.Network;
import com.freedomfinancestack.pos_sdk_core.enums.IssuerBank;

import java.util.Arrays;
import java.util.List;

public class ListSavedCards implements ICards {

    @Override
    public List<com.freedomfinancestack.pos_sdk_core.models.ListSavedCards> listAllSavedCards(String merchantId, String contact) {
        
        // Create mock cards based on the provided mock-card-token.txt
        Card card1 = Card.builder()
                .cardId("Ruv7t5fjSMKmp39j9CDD")
                .last4Digits("7623")
                .network(Network.RUPAY)
                .cardType(CardType.CREDIT)
                .issuerBank(IssuerBank.HDFC)
                .build();
        
        Card card2 = Card.builder()
                .cardId("qR24Mg6Q32N306Pg3vXr")
                .last4Digits("8477")
                .network(Network.VISA)
                .cardType(CardType.CREDIT)
                .issuerBank(IssuerBank.UTIB)
                .build();
        
        // Create the saved cards response
        com.freedomfinancestack.pos_sdk_core.models.ListSavedCards savedCardsResponse = 
                com.freedomfinancestack.pos_sdk_core.models.ListSavedCards.builder()
                .contact(contact != null ? contact : "+918955496900") // Use provided contact or default
                .cards(new Card[]{card1, card2})
                .build();
        
        return Arrays.asList(savedCardsResponse);
    }
}
