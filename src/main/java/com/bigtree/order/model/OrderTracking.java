package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("order-tracking")
@Data
@Builder
public class OrderTracking {

    @Id
    private String id;
    private String orderId;
    private String reference;
    private OrderStatus status;
    private LocalDateTime dateAccepted;
    private LocalDateTime datePaid;
    private LocalDateTime dateCancelled;
    private LocalDateTime dateDelivered;
    private LocalDateTime dateCollected;
    private LocalDateTime dateRefunded;
}
