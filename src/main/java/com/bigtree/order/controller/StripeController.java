package com.bigtree.order.controller;

import com.bigtree.order.model.LocalPaymentIntent;
import com.bigtree.order.model.PaymentIntentRequest;
import com.bigtree.order.service.StripeService;
import lombok.extern.slf4j.Slf4j;
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

    @CrossOrigin(origins = "*")
    @PostMapping("/payment-intent")
    public ResponseEntity<LocalPaymentIntent> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
        log.info("Request: createPaymentIntent: Amount:{}, Order:{}, Customer: {}", request.getAmount(), request.getOrderReference(), request.getCustomerEmail());
        LocalPaymentIntent response = stripeService.createPaymentIntent(request);
        return ResponseEntity.ok(response);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/payment-intent")
    public ResponseEntity<LocalPaymentIntent> getPaymentIntent(@RequestParam String orderReference) {
        log.info("Request: getPaymentIntent for Order: {}", orderReference);
        LocalPaymentIntent response = stripeService.getPaymentIntent(orderReference);
        return ResponseEntity.ok(response);
    }
}
