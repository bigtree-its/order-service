package com.bigtree.order.service;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.model.FoodOrder;
import com.bigtree.order.model.OrderStatus;
import com.bigtree.order.model.ProfileRequest;
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

    public ProfileResponse getProfile(ProfileRequest request){
        ProfileResponse response = ProfileResponse.builder().build();
        log.info("Processing profile request...");
        Query query = new Query();

        if ( StringUtils.isEmpty(request.getProfileType())){
            log.error("Profile type is mandatory");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Profile type is mandatory");
        }

        if ( StringUtils.isEmpty(request.getProfileEmail()) && StringUtils.isEmpty(request.getProfileId())){
            log.error("Either profile email or ID is mandatory");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Either profile email or ID is mandatory");
        }
        if (StringUtils.isNotEmpty(request.getProfileEmail())) {
            if ( request.getProfileType() == "Customer"){
                query.addCriteria(Criteria.where("customer.email").is(request.getProfileEmail()));
            }else if (request.getProfileType() == "Supplier"){
                query.addCriteria(Criteria.where("supplier.email").is(request.getProfileEmail()));
            }
        }
        if (StringUtils.isNotEmpty(request.getProfileId())) {
            if ( request.getProfileType() == "Customer"){
                query.addCriteria(Criteria.where("customer._id").is(request.getProfileId()));
            }else if (request.getProfileType() == "Supplier"){
                query.addCriteria(Criteria.where("supplier._id").is(request.getProfileId()));
            }
        }
        final LocalDate firstDayOfYear = Year.now().atMonth(1).atDay(1);
        if ( request.getDateFrom() == null && request.getDateTo() == null){
            query.addCriteria(Criteria.where("dateCreated").gte(firstDayOfYear));
        } else if (request.getDateFrom() != null && request.getDateTo() != null) {
            query.addCriteria(Criteria.where("dateCreated").gte(request.getDateFrom() ).lte(request.getDateTo()));
        } else if (request.getDateTo()  != null) {
            query.addCriteria(Criteria.where("dateCreated").lte(request.getDateTo() ));
        } else if (request.getDateFrom() != null) {
            query.addCriteria(Criteria.where("dateCreated").gte(request.getDateFrom()));
        }
        log.info("Searching orders with query {}", query);
        List<FoodOrder> orders = mongoTemplate.find(query, FoodOrder.class);
        if (!CollectionUtils.isEmpty(orders)){
            response = buildProfile(orders);
        }
        return response;
    }

    private ProfileResponse buildProfile(List<FoodOrder> orders){
        ProfileResponse response = ProfileResponse.builder().build();
        final LocalDate today = LocalDate.now();
        final LocalDate sevenDaysBefore = today.minusDays(7);
        final LocalDate firstDayOfCurrentMonth = YearMonth.now().atDay( 1 );
        final LocalDate firstDayOfPrevMonth = YearMonth.now().minusMonths(1).atDay(1);
        final LocalDate sixMonths = YearMonth.now().minusMonths(6).atDay(1);
        final LocalDate firstDayOfYear = Year.now().atMonth(1).atDay(1);

        for (FoodOrder order : orders) {
            if ( order.getDateCreated().isEqual(today)){
                response.getToday().add((order));
                response.getSevenDays().add((order));
                response.getMonth().add((order));
                response.getSixMonth().add(order);
                response.getYear().add(order);
            } else if ( order.getDateCreated().isEqual(sevenDaysBefore) || order.getDateCreated().isAfter(sevenDaysBefore) ){
                response.getSevenDays().add((order));
                response.getMonth().add((order));
                response.getSixMonth().add(order);
                response.getYear().add(order);
            } else if ( order.getDateCreated().isEqual(firstDayOfCurrentMonth) || order.getDateCreated().isAfter(firstDayOfCurrentMonth) ){
                response.getMonth().add((order));
                response.getSixMonth().add(order);
                response.getYear().add(order);
            } else if ( order.getDateCreated().isEqual(firstDayOfPrevMonth) || order.getDateCreated().isAfter(firstDayOfPrevMonth) ){
                response.getLastMonth().add(order);
                response.getSixMonth().add(order);
                response.getYear().add(order);
            }  else if ( order.getDateCreated().isEqual(sixMonths) || order.getDateCreated().isAfter(sixMonths) ){
                response.getSixMonth().add(order);
                response.getYear().add(order);
            } else if ( order.getDateCreated().isEqual(firstDayOfYear) || order.getDateCreated().isAfter(firstDayOfYear) ){
                response.getYear().add(order);
            } else{
                response.getDateRange().add((order));
            }
        }
        buildRevenueProfile(response);
        return response;
    }

    private ProfileResponse buildRevenueProfile(ProfileResponse response){

        BigDecimal today = BigDecimal.ZERO;
        BigDecimal sevenDaysRevenue = BigDecimal.ZERO;
        BigDecimal monthlyRevenue = BigDecimal.ZERO;
        BigDecimal lastMonthRevenue = BigDecimal.ZERO;
        BigDecimal sixMonthsRevenue = BigDecimal.ZERO;
        BigDecimal yearlyRevenue = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(response.getToday())){
            for (FoodOrder order : response.getToday()) {
                if ( order.getStatus() == OrderStatus.Paid){
                    today = today.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }

        if (!CollectionUtils.isEmpty(response.getSevenDays())){
            for (FoodOrder order : response.getSevenDays()) {
                if ( order.getStatus() == OrderStatus.Paid){
                    sevenDaysRevenue = sevenDaysRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }

        if (!CollectionUtils.isEmpty(response.getMonth())){
            for (FoodOrder order : response.getMonth()) {
                if ( order.getStatus() == OrderStatus.Paid){
                    monthlyRevenue = monthlyRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }
        if (!CollectionUtils.isEmpty(response.getLastMonth())){
            for (FoodOrder order : response.getLastMonth()) {
                if ( order.getStatus() == OrderStatus.Paid){
                    lastMonthRevenue = lastMonthRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }

        if (!CollectionUtils.isEmpty(response.getSixMonth())){
            for (FoodOrder order : response.getSixMonth()) {
                if ( order.getStatus() == OrderStatus.Paid){
                    sixMonthsRevenue = sixMonthsRevenue.add(order.getTotal().subtract(order.getServiceFee()));
                }
            }
        }
        if (!CollectionUtils.isEmpty(response.getYear())){
            for (FoodOrder order : response.getYear()) {
                if ( order.getStatus() == OrderStatus.Paid){
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
        return  response;
    }

}
