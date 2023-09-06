package com.bigtree.beku.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentIntentRequest {

    private String supplierId;
    private BigDecimal amount;
    private String currency;
    private String orderReference;
    private String customerEmail;
}
