package com.bigtree.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/customer-orders")
public class StatusController {
    

    @GetMapping("status")
    public String status(){
        log.info("Rqeuest for status");
        return "Working !";
    }
}
