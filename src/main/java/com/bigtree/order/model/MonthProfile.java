package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;

@Data
@Builder
public class MonthProfile {
    private Month month;
    private BigDecimal revenue;
    private Integer count;
    private List<OrderDTO> orders;
}
