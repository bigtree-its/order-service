package com.bigtree.order.controller;


import com.bigtree.order.model.FoodOrder;
import com.bigtree.order.model.ProfileResponse;
import com.bigtree.order.service.FoodOrderService;
import com.bigtree.order.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@CrossOrigin(exposedHeaders =
        {"Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"},
        allowedHeaders = {"Authorization", "Origin"},
        origins = {"http://localhost:4200", "*"})
@RequestMapping("/kitchen-orders/v1")
public class KitchenOrderController {

    @Autowired
    ProfileService profileService;

    @Autowired
    FoodOrderService orderService;

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getOrderProfile(
            @RequestParam(value = "customer", required = false) String customer,
            @RequestParam(value = "cloudKitchenId", required = false) String cloudKitchenId,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDate dateTo
    ) {
        log.info("Request to get profile for {}", cloudKitchenId);
        final ProfileResponse response = profileService.getProfile(customer, cloudKitchenId, date, dateFrom, dateTo);
        log.info("Returning profile response");
        return ResponseEntity.ok(response);
    }

    @PutMapping("")
    public ResponseEntity<FoodOrder> action(@RequestParam String ref, @RequestParam String action) {
        log.info("Request to {} order  {}", action, ref);
        FoodOrder foodOrder = orderService.action(ref, action);
        return ResponseEntity.ok(foodOrder);
    }
}
