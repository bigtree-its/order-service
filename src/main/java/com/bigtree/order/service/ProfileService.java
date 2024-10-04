package com.bigtree.order.service;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.helper.MonthUtils;
import com.bigtree.order.model.*;
import com.bigtree.order.repository.SalesRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Slf4j
@Service
public class ProfileService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    SalesRepository salesRepository;

    public SalesProfile getSalesProfile(String cloudKitchenId){

        SalesProfile salesProfile = SalesProfile.builder().build();
        final LocalDate firstDayOfYear = LocalDate.now()
                .withDayOfMonth(1)
                .withMonth(1)
                .minusYears(1);
        MatchOperation matchOperation1 = match(new Criteria("cloudKitchen._id").is(cloudKitchenId));
        MatchOperation matchOperation2 = match(new Criteria("dateCreated").gte(firstDayOfYear));
        ProjectionOperation projectionOperation = project("reference", "total", "dateCreated", "status").and("cloudKitchen._id").as("cloudKitchenId");
        TypedAggregation<FoodOrder> agg = newAggregation(FoodOrder.class, matchOperation1,matchOperation2, projectionOperation );
        AggregationResults<OrderDTO> result = mongoTemplate.aggregate(agg, OrderDTO.class);
        List<OrderDTO> documents = result.getMappedResults();

        Map<Year, List<OrderDTO>> byYear = documents.stream()
                .collect(Collectors.groupingBy(m -> Year.from(m.getDateCreated()), Collectors.toList()));


        for (Map.Entry<Year, List<OrderDTO>> entry : byYear.entrySet()) {
            YearProfile yearProfile = YearProfile.builder().build();
            yearProfile.setYear(entry.getKey());
            BigDecimal sum = entry.getValue().stream().map(OrderDTO::getTotal).reduce(BigDecimal.valueOf(0), BigDecimal::add);
            yearProfile.setRevenue(sum);
            yearProfile.setCount(entry.getValue().size());
            yearProfile.setMonthlyProfiles(new ArrayList<>());
            Map<Month, List<OrderDTO>> byMonth = entry.getValue().stream()
                    .collect(Collectors.groupingBy(m -> Month.from(m.getDateCreated()), Collectors.toList()));
            for (Map.Entry<Month, List<OrderDTO>> monthlyEntry : byMonth.entrySet()) {
                MonthProfile monthProfile = MonthProfile.builder().build();
                monthProfile.setMonth(MonthUtils.getShortName(monthlyEntry.getKey()));
                monthProfile.setOrders(entry.getValue());
                BigDecimal monthlySum = monthlyEntry.getValue().stream().map(OrderDTO::getTotal).reduce(BigDecimal.valueOf(0), BigDecimal::add);
                monthProfile.setRevenue(monthlySum);
                monthProfile.setCount(monthlyEntry.getValue().size());
                yearProfile.getMonthlyProfiles().add(monthProfile);
            }
            List<MonthProfile> missing = new ArrayList<>();
            Arrays.stream(MonthUtils.getMonths()).forEach(m-> {
                if ( yearProfile.getMonthlyProfiles().stream().noneMatch(p-> p.getMonth().equalsIgnoreCase(m))){
                    missing.add(MonthProfile.builder().month(m).count(0).revenue(BigDecimal.ZERO).orders(new ArrayList<>()).build());
                }
            });
            yearProfile.getMonthlyProfiles().addAll(missing);
            if (Objects.equals(yearProfile.getYear(), Year.now().minusYears(1))){
                salesProfile.setPrevious(yearProfile);
            }
            if (Objects.equals(yearProfile.getYear(), Year.now())){
                salesProfile.setCurrent(yearProfile);
            }
            if ( salesProfile.getPrevious() == null){
                salesProfile.setPrevious(YearProfile.builder().year(Year.now().minusYears(1)).revenue(BigDecimal.ZERO).count(0).monthlyProfiles(new ArrayList<>()).build());
            }
            if ( salesProfile.getCurrent() == null){
                salesProfile.setPrevious(YearProfile.builder().year(Year.now()).revenue(BigDecimal.ZERO).count(0).monthlyProfiles(new ArrayList<>()).build());
            }
        }
        final LocalDate today = LocalDate.now();
        final LocalDate sevenDaysBefore = today.minusDays(7);
        final LocalDate firstDayOfCurrentMonth = YearMonth.now().atDay(1);
        final LocalDate firstDayOfPrevMonth = YearMonth.now().minusMonths(1).atDay(1);
        final LocalDate sixMonths = YearMonth.now().minusMonths(6).atDay(1);
        for (OrderDTO order : documents) {
            if (order.getDateCreated().isEqual(today)) {
                salesProfile.getToday().add((order));
                salesProfile.getSevenDays().add((order));
                salesProfile.getMonth().add((order));
                salesProfile.getSixMonth().add(order);
            } else if (order.getDateCreated().isEqual(sevenDaysBefore) || order.getDateCreated().isAfter(sevenDaysBefore)) {
                salesProfile.getSevenDays().add((order));
                salesProfile.getMonth().add((order));
                salesProfile.getSixMonth().add(order);
            } else if (order.getDateCreated().isEqual(firstDayOfCurrentMonth) || order.getDateCreated().isAfter(firstDayOfCurrentMonth)) {
                salesProfile.getMonth().add((order));
                salesProfile.getSixMonth().add(order);
            } else if (order.getDateCreated().isEqual(firstDayOfPrevMonth) || order.getDateCreated().isAfter(firstDayOfPrevMonth)) {
                salesProfile.getLastMonth().add(order);
                salesProfile.getSixMonth().add(order);
            } else if (order.getDateCreated().isEqual(sixMonths) || order.getDateCreated().isAfter(sixMonths)) {
                salesProfile.getSixMonth().add(order);
            }
        }
        return salesProfile;

    }

    public ProfileResponse getProfile(String customer, String cloudKitchenId, LocalDate date, LocalDate dateFrom, LocalDate dateTo) {
        ProfileResponse response = ProfileResponse.builder().build();
        log.info("Processing profile request...");
        Query query = new Query();

        if (StringUtils.isEmpty(customer) && StringUtils.isEmpty(cloudKitchenId)) {
            log.error("CloudKitchen Id or Customer email is mandatory");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "CloudKitchen Id or Customer email is mandatory");
        }
        if (StringUtils.isNotEmpty(cloudKitchenId)) {
            query.addCriteria(Criteria.where("cloudKitchen._id").is(cloudKitchenId));
        }
        if (StringUtils.isNotEmpty(customer)) {
            query.addCriteria(Criteria.where("customer.email").is(customer));

        }
        final LocalDate firstDayOfYear = LocalDate.now()
                .withDayOfMonth(1)
                .withMonth(1)
                .minusYears(1);
//        final LocalDate firstDayOfYear = Year.now().atMonth(1).atDay(1);
        if (date != null) {
            query.addCriteria(Criteria.where("dateCreated").gte(date));
        }
        if (dateFrom == null && dateTo == null) {
            query.addCriteria(Criteria.where("dateCreated").gte(firstDayOfYear));
        } else if (dateFrom != null) {
            if (dateTo != null) {
                query.addCriteria(Criteria.where("dateCreated").gte(dateFrom).lte(dateTo));
            } else {
                query.addCriteria(Criteria.where("dateCreated").gte(dateFrom));
            }
        } else {
            query.addCriteria(Criteria.where("dateCreated").lte(dateTo));
        }
        log.info("Searching orders with query {}", query);
        List<FoodOrder> orders = mongoTemplate.find(query, FoodOrder.class);
        mongoTemplate.count(query, FoodOrder.class);
        if (!CollectionUtils.isEmpty(orders)) {
            response = buildProfile(orders);
        }
        return response;
    }

    private ProfileResponse buildProfile(List<FoodOrder> orders) {
        ProfileResponse response = ProfileResponse.builder().build();
        final LocalDate today = LocalDate.now();
        final LocalDate sevenDaysBefore = today.minusDays(7);
        final LocalDate firstDayOfCurrentMonth = YearMonth.now().atDay(1);
        final LocalDate firstDayOfPrevMonth = YearMonth.now().minusMonths(1).atDay(1);
        final LocalDate sixMonths = YearMonth.now().minusMonths(6).atDay(1);
        final LocalDate firstDayOfYear = Year.now().atMonth(1).atDay(1);
        Map<YearMonth, List<FoodOrder>> ordersByMonth = orders.stream()
                .collect(Collectors.groupingBy(m -> YearMonth.from(m.getDateCreated()), Collectors.toList()));
        response.setAll(orders);
        response.setOrdersByMonth(ordersByMonth);
        for (FoodOrder order : orders) {
            if (order.getDateCreated().isEqual(today)) {
                response.getToday().add((order));
                response.getSevenDays().add((order));
                response.getMonth().add((order));
                response.getSixMonth().add(order);
                response.getYear().add(order);
            } else if (order.getDateCreated().isEqual(sevenDaysBefore) || order.getDateCreated().isAfter(sevenDaysBefore)) {
                response.getSevenDays().add((order));
                response.getMonth().add((order));
                response.getSixMonth().add(order);
                response.getYear().add(order);
            } else if (order.getDateCreated().isEqual(firstDayOfCurrentMonth) || order.getDateCreated().isAfter(firstDayOfCurrentMonth)) {
                response.getMonth().add((order));
                response.getSixMonth().add(order);
                response.getYear().add(order);
            } else if (order.getDateCreated().isEqual(firstDayOfPrevMonth) || order.getDateCreated().isAfter(firstDayOfPrevMonth)) {
                response.getLastMonth().add(order);
                response.getSixMonth().add(order);
                response.getYear().add(order);
            } else if (order.getDateCreated().isEqual(sixMonths) || order.getDateCreated().isAfter(sixMonths)) {
                response.getSixMonth().add(order);
                response.getYear().add(order);
            } else if (order.getDateCreated().isEqual(firstDayOfYear) || order.getDateCreated().isAfter(firstDayOfYear)) {
                response.getYear().add(order);
            } else {
                response.getDateRange().add((order));
            }
        }
        buildRevenueProfile(response);
        return response;
    }

    private ProfileResponse buildRevenueProfile(ProfileResponse response) {

        BigDecimal today = BigDecimal.ZERO;
        BigDecimal sevenDaysRevenue = BigDecimal.ZERO;
        BigDecimal monthlyRevenue = BigDecimal.ZERO;
        BigDecimal lastMonthRevenue = BigDecimal.ZERO;
        BigDecimal sixMonthsRevenue = BigDecimal.ZERO;
        BigDecimal yearlyRevenue = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(response.getToday())) {
            for (FoodOrder order : response.getToday()) {
                if (order.getStatus().equalsIgnoreCase(OrderStatus.Open)) {
                    today = today.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }

        if (!CollectionUtils.isEmpty(response.getSevenDays())) {
            for (FoodOrder order : response.getSevenDays()) {
                if (order.getStatus().equalsIgnoreCase(OrderStatus.Open)) {
                    sevenDaysRevenue = sevenDaysRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }

        if (!CollectionUtils.isEmpty(response.getMonth())) {
            for (FoodOrder order : response.getMonth()) {
                if (order.getStatus().equalsIgnoreCase(OrderStatus.Open)) {
                    monthlyRevenue = monthlyRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }
        if (!CollectionUtils.isEmpty(response.getLastMonth())) {
            for (FoodOrder order : response.getLastMonth()) {
                if (order.getStatus().equalsIgnoreCase(OrderStatus.Open)) {
                    lastMonthRevenue = lastMonthRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }

        if (!CollectionUtils.isEmpty(response.getSixMonth())) {
            for (FoodOrder order : response.getSixMonth()) {
                if (order.getStatus().equalsIgnoreCase(OrderStatus.Open)) {
                    sixMonthsRevenue = sixMonthsRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }
        if (!CollectionUtils.isEmpty(response.getYear())) {
            for (FoodOrder order : response.getYear()) {
                if (order.getStatus().equalsIgnoreCase(OrderStatus.Open)) {
                    yearlyRevenue = yearlyRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }

        response.setTodayRevenue(today);
        response.setSevenDaysRevenue(sevenDaysRevenue);
        response.setMonthRevenue(monthlyRevenue);
        response.setLastMonthRevenue(lastMonthRevenue);
        response.setSixMonthsRevenue(sixMonthsRevenue);
        response.setYearRevenue(yearlyRevenue);
        return response;
    }

}
