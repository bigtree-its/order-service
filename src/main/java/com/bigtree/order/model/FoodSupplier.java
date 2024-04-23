package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FoodSupplier {

    private String _id;
    private String name;
    private String tradingName;
    private String image;
    private String mobile;
    private String email;
    private Address address;
}
