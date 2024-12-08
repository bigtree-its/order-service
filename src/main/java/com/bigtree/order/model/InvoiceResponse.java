package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceResponse {
    private String cloudKitchenId;
    private LocalDate date;
    private LocalDate dateAccepted;
    private LocalDate dateCompleted;
    private String status;
    private String orderReference;
    private BigDecimal orderAmount;
    private BigDecimal invoiceAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}
