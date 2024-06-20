package com.bigtree.order.service;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.model.FoodOrder;
import com.bigtree.order.model.OrderStatus;
import com.bigtree.order.model.ProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
public class ProfileService {

    @Autowired
    MongoTemplate mongoTemplate;

    public ProfileResponse getProfile(String customer, String supplier, LocalDate date, LocalDate dateFrom, LocalDate dateTo) {
        ProfileResponse response = ProfileResponse.builder().build();
        log.info("Processing profile request...");
        Query query = new Query();

        if (StringUtils.isEmpty(customer) && StringUtils.isEmpty(supplier)) {
            log.error("Supplier or Customer email is mandatory");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Supplier or Customer email is mandatory");
        }
        if (StringUtils.isNotEmpty(supplier)) {
            query.addCriteria(Criteria.where("supplier.email").is(supplier));

        }
        if (StringUtils.isNotEmpty(customer)) {
            query.addCriteria(Criteria.where("customer.email").is(customer));

        }
        final LocalDate firstDayOfYear = Year.now().atMonth(1).atDay(1);
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
                if (order.getStatus() == OrderStatus.Paid) {
                    today = today.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }

        if (!CollectionUtils.isEmpty(response.getSevenDays())) {
            for (FoodOrder order : response.getSevenDays()) {
                if (order.getStatus() == OrderStatus.Paid) {
                    sevenDaysRevenue = sevenDaysRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }

        if (!CollectionUtils.isEmpty(response.getMonth())) {
            for (FoodOrder order : response.getMonth()) {
                if (order.getStatus() == OrderStatus.Paid) {
                    monthlyRevenue = monthlyRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }
        if (!CollectionUtils.isEmpty(response.getLastMonth())) {
            for (FoodOrder order : response.getLastMonth()) {
                if (order.getStatus() == OrderStatus.Paid) {
                    lastMonthRevenue = lastMonthRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }

        if (!CollectionUtils.isEmpty(response.getSixMonth())) {
            for (FoodOrder order : response.getSixMonth()) {
                if (order.getStatus() == OrderStatus.Paid) {
                    sixMonthsRevenue = sixMonthsRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }
        if (!CollectionUtils.isEmpty(response.getYear())) {
            for (FoodOrder order : response.getYear()) {
                if (order.getStatus() == OrderStatus.Paid) {
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
