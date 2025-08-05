package org.freedomfinacestack.razorpay.drishtipay.models;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;

import java.math.BigInteger;

@Builder
@Data
@AllArgsConstructor
public class CardToken {
    private String id;
    private BigInteger createdAt;
    private String merchantId;


} 