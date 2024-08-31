package com.bigtree.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    private String id;
    private String _tempId;
    private String cloudKitchenId;
    private String collectionId;
    private String name;
    private String slug;
    private BigDecimal price;
    private BigDecimal spice;
    private BigDecimal quantity;
    private BigDecimal subTotal;
    private String image;
    private String specialInstruction;
    private List<Extra> extras;
    private List<Extra> choices;
    private Extra choice;
    private boolean vegetarian;

}
