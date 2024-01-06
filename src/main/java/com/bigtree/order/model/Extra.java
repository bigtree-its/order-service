package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class Extra {

    private String name;
    private BigDecimal price;
    private Integer quantity;
}
