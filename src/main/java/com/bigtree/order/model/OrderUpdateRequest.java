package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderUpdateRequest {

    String reference;
    String id;
    String paymentIntentId;
    String paymentStatus;
    String status;
    String chefNotes;
    String customerComments;
    LocalDate expectedCollectionDate;
    LocalDate expectedDeliveryDate;
    Integer customerRating;
}
