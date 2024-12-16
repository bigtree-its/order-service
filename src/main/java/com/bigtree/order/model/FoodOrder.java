package com.bigtree.order.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "food-orders")
public class FoodOrder {

    @Id
    private String _id;
    private String paymentIntentId;
    private String clientSecret;
    private String reference;
    private List<Note> customerNotes;
    private List<Note>  kitchenNotes;
    private String customerComment;
    private Integer customerRating;
    private String currency;
    private String status;
    private String kitchenAction;
    private ServiceMode serviceMode;
    private Customer customer;
    private CloudKitchen cloudKitchen;
    private boolean preOrder;
    private boolean partyOrder;
    private List<Item> items;
    private List<PartyItem> partyItems;
    private BigDecimal subTotal;
    private BigDecimal total;
    private BigDecimal serviceFee;
    private BigDecimal deliveryFee;
    private BigDecimal packingFee;
    private LocalDate partyDate;
    private String partyTime;
    private LocalDate orderDate;
    private LocalDateTime scheduledDate;
    private LocalDate expectedDeliveryDate;
    private LocalDateTime dateCreated;
    private LocalDateTime dateAccepted;
    private LocalDateTime dateDelivered;
    private LocalDateTime dateCancelled;
    private LocalDateTime dateCollected;
    private LocalDateTime dateDeclined;
    private LocalDateTime dateReady;
    private LocalDateTime dateTransit;
    private LocalDateTime dateRefunded;
    private LocalDateTime dateRefundStarted;
    private LocalDateTime datePaid;
    private LocalDateTime dateInvoiced;
    private LocalDateTime dateInvoiceAccepted;
    private LocalDateTime dateCompleted;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

}
