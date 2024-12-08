package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "invoices")
@Data
@Builder
public class Invoice {

    @Id
    private String id;
    private String cloudKitchenId;
    private LocalDate date;
    private LocalDate dateAccepted;
    private LocalDate dateCompleted;
    private String status;
    private String orderReference;
    private BigDecimal orderAmount;
    private BigDecimal invoiceAmount;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}
