package org.freedomfinacestack.razorpay.drishtipay.models;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;

@Builder
@Data
@AllArgsConstructor
public class ListSavedCards {
    private String contact;
    private Card[] cards;
} 