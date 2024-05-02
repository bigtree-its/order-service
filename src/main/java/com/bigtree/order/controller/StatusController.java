package com.bigtree.order.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/orders/v1/status")
public class StatusController {
    

    @GetMapping("")
    public String status(){
        log.info("Request for status");
        return "Working !";
    }
}
