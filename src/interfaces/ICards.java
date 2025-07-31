package interfaces;

import models.ListSavedCards;

public interface ICards {
    // List all the saved cards for  the merchant and contact id
    ListSavedCards[] ListAllSavedCards(String merchantId, String contact);
}



