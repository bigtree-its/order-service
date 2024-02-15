package com.bigtree.order.controller;

import com.bigtree.order.model.LocalPaymentIntent;
import com.bigtree.order.model.PaymentIntentRequest;
import com.bigtree.order.service.StripeService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/stripe-payments")
@CrossOrigin(origins = "*")
public class StripeController {


    @Autowired
    StripeService stripeService;

    @PostMapping("/payment-intent")
    public ResponseEntity<LocalPaymentIntent> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
        log.info("Request: createPaymentIntent: Amount:{}, Order:{}, Customer: {}", request.getAmount(), request.getOrderReference(), request.getCustomerEmail());
        LocalPaymentIntent response = stripeService.createPaymentIntent(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment-intent")
    public ResponseEntity<List<LocalPaymentIntent>> getAll(@RequestParam(required = false, value = "orderReference") String orderReference) {
        log.info("Request: getPaymentIntent for order reference {}", orderReference);
        List<LocalPaymentIntent> response = stripeService.lookup(orderReference);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment-intent/{intentId}")
    public ResponseEntity<LocalPaymentIntent> getSingle(@PathVariable String intentId) {
        log.info("Request: getPaymentIntent with intent id ");
        LocalPaymentIntent response = stripeService.getPaymentIntentById(intentId);
        return ResponseEntity.ok(response);
    }


}
