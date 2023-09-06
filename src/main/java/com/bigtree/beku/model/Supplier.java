package com.bigtree.beku.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Supplier {

    private UUID id;
    private String name;
    private String image;
    private String mobile;
    private String email;
    private Address address;
}
