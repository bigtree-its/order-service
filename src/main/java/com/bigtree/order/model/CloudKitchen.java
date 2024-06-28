package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloudKitchen {

    private String _id;
    private String name;
    private String image;
    private String mobile;
    private String email;
    private Address address;
}
