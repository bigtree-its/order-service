package com.bigtree.order.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Document(collection = "customer-orders")
public class CustomerOrder{

    @Id
    private String _id;
    private String reference;
    private String notes;
    private String customerComment;
    private Integer customerRating;
    private String currency;
    private OrderStatus status;
    private ServiceMode serviceMode;
    private Customer customer;
    private Supplier supplier;
    private List<Item> items;
    private BigDecimal subTotal;
    private BigDecimal total;
    private BigDecimal serviceFee;
    private BigDecimal deliveryFee;
    private BigDecimal packingFee;
    private LocalDate dateCreated;
    private LocalDate collectBy;
    private LocalDate expectedDeliveryDate;
    private LocalDateTime dateAccepted;
    private LocalDateTime dateDelivered;
    private LocalDateTime dateCollected;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
