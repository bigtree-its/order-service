package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class SalesProfile {
    private YearProfile previous;
    private YearProfile current;
    @Builder.Default
    private List<OrderDTO> today = new ArrayList<>();
    @Builder.Default
    private List<OrderDTO> sevenDays = new ArrayList<>();
    @Builder.Default
    private List<OrderDTO> sixMonth = new ArrayList<>();
    @Builder.Default
    private List<OrderDTO> month =new ArrayList<>();
    @Builder.Default
    private List<OrderDTO> lastMonth =new ArrayList<>();
}
