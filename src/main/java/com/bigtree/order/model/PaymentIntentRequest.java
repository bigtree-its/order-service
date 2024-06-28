package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentIntentRequest {

    private String cloudKitchenId;
    private BigDecimal amount;
    private String currency;
    private String orderReference;
    private String customerEmail;
}
