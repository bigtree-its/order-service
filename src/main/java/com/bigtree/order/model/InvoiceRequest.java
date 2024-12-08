package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceRequest {
    private String action;
    private String cloudKitchenId;
    private String orderReference;
}
