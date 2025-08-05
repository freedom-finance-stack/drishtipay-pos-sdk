package com.freedomfinancestack.pos_sdk_core.models;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;


@Builder
@Data
@AllArgsConstructor
public class PaymentInitiationResponse {
    private String acsURL;
    private String paymentId;
    private String orderId;
} 