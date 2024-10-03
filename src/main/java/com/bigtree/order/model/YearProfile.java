package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

@Data
@Builder
public class YearProfile {
    private Year year;
    private BigDecimal revenue;
    private Integer count;
    private List<MonthProfile> monthlyProfiles;
}
