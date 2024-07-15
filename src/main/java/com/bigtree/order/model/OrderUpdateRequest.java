package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class OrderUpdateRequest {

    String reference;
    String id;
    String paymentIntentId;
    String paymentStatus;
    String status;
    private List<Note> customerNotes;
    private List<Note>  kitchenNotes;
    String customerComments;
    LocalDate expectedCollectionDate;
    LocalDate expectedDeliveryDate;
    Integer customerRating;
}
