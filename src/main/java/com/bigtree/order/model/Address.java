package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Address {

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postcode;
    private String country;
    private String latitude;
    private String longitude;
}
