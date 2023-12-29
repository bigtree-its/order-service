package com.bigtree.beku.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Customer {

    private String _id;
    private String name;
    private String mobile;
    private String email;
    private Address address;
}
