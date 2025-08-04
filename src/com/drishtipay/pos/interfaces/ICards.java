package com.drishtipay.pos.interfaces;

import com.drishtipay.pos.models.ListSavedCards;

import java.util.List;

public interface ICards {
    // List all the saved cards for  the merchant and contact id
    List<ListSavedCards> listAllSavedCards(String merchantId, String contact);
}



