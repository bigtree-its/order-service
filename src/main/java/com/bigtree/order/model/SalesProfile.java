package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalesProfile {
    private YearProfile previous;
    private YearProfile current;
}
