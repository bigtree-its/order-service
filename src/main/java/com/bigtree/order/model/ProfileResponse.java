package com.bigtree.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Builder
@Data @AllArgsConstructor
public class ProfileResponse {

    @Builder.Default
    private List<FoodOrder> all = new ArrayList<>();
    @Builder.Default
    private List<FoodOrder> today = new ArrayList<>();
    @Builder.Default
    private List<FoodOrder> sevenDays = new ArrayList<>();
    @Builder.Default
    private List<FoodOrder> month =new ArrayList<>();
    @Builder.Default
    private List<FoodOrder> lastMonth = new ArrayList<>();
    @Builder.Default
    private List<FoodOrder> sixMonth = new ArrayList<>();
    @Builder.Default
    private List<FoodOrder> year = new ArrayList<>();
    @Builder.Default
    private List<FoodOrder> dateRange = new ArrayList<>();
    private LocalDate dateFrom;
    private LocalDate dateTp;
    private BigDecimal todayRevenue;
    private BigDecimal sevenDaysRevenue;
    private BigDecimal monthRevenue;
    private BigDecimal lastMonthRevenue;
    private BigDecimal sixMonthsRevenue;
    private BigDecimal yearRevenue;
    private Map<YearMonth, List<FoodOrder>> ordersByMonth;
}
