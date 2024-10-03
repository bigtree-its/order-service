package com.bigtree.order.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrderDTO {
    private String _id;
    private LocalDate dateCreated;
    private String reference;
    private String status;
    private String cloudKitchenId;
    private BigDecimal total;
}
