package com.bigtree.order.controller;

import com.bigtree.order.model.Payment;
import com.bigtree.order.model.PaymentIntentRequest;
import com.bigtree.order.service.StripeService;
import com.stripe.model.PaymentIntent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/orders/v1/foods/stripe-payments")
@CrossOrigin(origins = "*")
public class StripeController {


    @Autowired
    StripeService stripeService;

    @PostMapping("/payment-intent")
    public ResponseEntity<PaymentIntent> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
        log.info("Request: createPaymentIntent: Amount:{}, Order:{}, Customer: {}", request.getAmount(), request.getOrderReference(), request.getCustomerEmail());
        PaymentIntent response = stripeService.createPaymentIntent(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment-intent")
    public ResponseEntity<List<Payment>> getAll(@RequestParam(required = false, value = "ref") String ref,
                                                @RequestParam(required = false, value = "intent") String intent,
                                                @RequestParam(required = false, value = "status") String status) {
        log.info("Request: Lookup payment intents");
        List<Payment> response = stripeService.lookup(ref, status, intent);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment-intent/{intentId}")
    public ResponseEntity<Payment> getSingle(@PathVariable String intentId) {
        log.info("Request: getPaymentIntent with intent id ");
        Payment response = stripeService.retrievePayment(intentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/payment-intent")
    public ResponseEntity<Void> deleteAllOrders() {
        log.info("Request to delete all payments");
        stripeService.deleteAll();
        return ResponseEntity.accepted().build();
    }

}
