package com.bigtree.order.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderDTO {
    private String _id;
    private LocalDate orderDate;
    private LocalDateTime dateCreated;
    private String reference;
    private String status;
    private String cloudKitchenId;
    private BigDecimal total;
}
