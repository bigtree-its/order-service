package com.bigtree.beku.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class Item {

    private String id;
    private String _tempId;
    private String name;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal subTotal;
    private String image;
    private String specialInstruction;
    private List<Extra> extras;
    private Extra choice;

}
