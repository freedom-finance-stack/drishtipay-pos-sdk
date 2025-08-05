package com.freedomfinancestack.pos_sdk_core.models;

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