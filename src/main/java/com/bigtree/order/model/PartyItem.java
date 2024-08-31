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
public class PartyItem {

    private String _id;
    private String name;
    private String image;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal subTotal;
    private List<PartyItemCandidate> candidates;
}
