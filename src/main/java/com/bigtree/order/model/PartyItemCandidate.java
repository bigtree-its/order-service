package com.bigtree.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyItemCandidate {

    private String name;
    private boolean required;
    private int max;
    private List<Item> items;
}
