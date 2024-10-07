package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Year;

@Data
@Builder
public class YearProfile {
    private Year year;
    private BigDecimal revenue;
    private Integer count;
    private MonthProfile jan;
    private MonthProfile feb;
    private MonthProfile mar;
    private MonthProfile apr;
    private MonthProfile may;
    private MonthProfile jun;
    private MonthProfile jul;
    private MonthProfile aug;
    private MonthProfile sep;
    private MonthProfile oct;
    private MonthProfile nov;
    private MonthProfile dec;
}
