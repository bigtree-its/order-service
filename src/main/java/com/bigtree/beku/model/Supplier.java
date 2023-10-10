package com.bigtree.beku.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Supplier {

    private String _id;
    private String name;
    private String image;
    private String mobile;
    private String email;
    private Address address;
}
