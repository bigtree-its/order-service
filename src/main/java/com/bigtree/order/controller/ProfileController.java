package com.bigtree.order.controller;


import com.bigtree.order.model.ProfileRequest;
import com.bigtree.order.model.ProfileResponse;
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
@RequestMapping("/orders/v1/profiles")
public class ProfileController {

    @Autowired
    ProfileService profileService;

    @GetMapping("")
    public ResponseEntity<ProfileResponse> getOrderProfile(
            @RequestParam(value = "customer", required = false) String customer,
            @RequestParam(value = "supplier", required = false) String supplier,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDate dateTo
    ) {
        log.info("Request to get profile for {}", supplier);
        final ProfileResponse response = profileService.getProfile(customer, supplier, date, dateFrom, dateTo);
        log.info("Returning profile response");
        return ResponseEntity.ok(response);
    }
}
