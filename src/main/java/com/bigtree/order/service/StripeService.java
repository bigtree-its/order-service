package com.bigtree.order.service;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.model.Payment;
import com.bigtree.order.model.PaymentIntentRequest;
import com.bigtree.order.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StripeService {

    @Value("${stripe.private.key}")
    private String stripeKey;
    @Value("${stripe.success.url}")
    private String successUrl;
    @Value("${stripe.cancel.url}")
    private String cancelUrl;
    @Value("${stripe.currency}")
    private String currency;
    final BigDecimal hundred = new BigDecimal(100);

    @Autowired
    PaymentRepository paymentRepository;

    public List<Payment> lookup(String orderReference, String status, String intent) {
        List<Payment> result = new ArrayList<>();
        if (StringUtils.isNotEmpty(orderReference)) {
            log.info("Retrieving payment for orderReference {}", orderReference);
            Payment payment = paymentRepository.findFirstByOrderReference(orderReference);
            if (payment != null) {
                result.add(payment);
            }
        }
        if (StringUtils.isNotEmpty(intent)) {
            log.info("Retrieving payment intent for id {}", intent);
            Payment payment = paymentRepository.findFirstByIntentId(intent);
            if (payment != null) {
                result.add(payment);
            }
        }
        if (StringUtils.isNotEmpty(status)) {
            log.info("Retrieving PaymentIntent with status {}", status);
            List<Payment> list = paymentRepository.findByStatus(status);
            if (!CollectionUtils.isEmpty(list)) {
                return list;
            }
        }
        if (CollectionUtils.isEmpty(result)) {
            return paymentRepository.findAll();
        }
        return result;
    }

    public Payment createPaymentIntent(PaymentIntentRequest request) {
        Stripe.apiKey = stripeKey;

        Payment payment = paymentRepository.findFirstByOrderReference(request.getOrderReference());
        if ( payment != null){
            final PaymentIntent paymentIntent = retrieveStripePaymentIntent(payment.getIntentId());
            if ( paymentIntent == null){
                return createStripePaymentIntent(request);
            }else{
                if (paymentIntent.getStatus().equalsIgnoreCase("succeeded")) {
                    log.error("Existing intent {} in stripe is succeeded. Cannot update anymore.", paymentIntent.getId());
                    throw  new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Order is fully Paid. Cannot update further");
                }else{
                    updatePaymentIntent(request, paymentIntent);
                }
            }
        }else{
            payment =  createStripePaymentIntent(request);
        }
        return payment;
    }

    private Payment createStripePaymentIntent(PaymentIntentRequest request) {
        final BigDecimal stripeAmount = request.getAmount().multiply(hundred);
        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setCurrency(currency)
                        .setAmount(stripeAmount.longValue())
                        // Verify your integration in this guide by including this parameter
                        .putMetadata("integration_check", "accept_a_payment")
                        .putMetadata("order", request.getOrderReference())
                        .build();
        try {
            PaymentIntent created = PaymentIntent.create(params);
            log.info("Created new payment intent in Stripe " + created.getId());
            return saveNewLocalPaymentIntent(request, created);
        } catch (StripeException e) {
            log.error("Unable to create payment intent {}", e.getMessage());
            return Payment.builder()
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    public void updatePaymentIntent(PaymentIntentRequest request, PaymentIntent paymentIntent) {
        try {
            final BigDecimal stripeAmount = request.getAmount().multiply(hundred);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("amount", stripeAmount.longValue());
            Map<String, Object> params = new HashMap<>();
            params.put("metadata", metadata);
            params.put("amount", stripeAmount.longValue());
            paymentIntent.update(params);
            log.info("Payment Intent {} is updated for order {}", paymentIntent.getId(), request.getOrderReference());
        } catch (StripeException e) {
            throw  new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage());
        }
    }


    private Payment saveNewLocalPaymentIntent(PaymentIntentRequest request, PaymentIntent stripeIntent) {
        final Payment newPayment = paymentRepository.save(Payment.builder()
                .intentId(stripeIntent.getId())
                .orderReference(request.getOrderReference())
                .build());
        log.info("Created payment intent copy in local for order {}", newPayment.getOrderReference());
        return newPayment;
    }

    public Payment retrievePayment(String intentId) {

        if (StringUtils.isEmpty(intentId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Intent ID is mandatory");
        }
        final PaymentIntent paymentIntent = retrieveStripePaymentIntent(intentId);
        final Payment payment = paymentRepository.findFirstByIntentId(intentId);
        if ( paymentIntent != null){
            payment.setAmount(BigDecimal.valueOf(paymentIntent.getAmount()));
            payment.setPaymentMethod(paymentIntent.getPaymentMethod());
            payment.setCurrency(paymentIntent.getCurrency());
            payment.setStatus(paymentIntent.getStatus());
            payment.setClientSecret(paymentIntent.getClientSecret());
            payment.setError(paymentIntent.getLastPaymentError() != null? paymentIntent.getLastPaymentError() .getMessage(): null);
            payment.setLiveMode(paymentIntent.getLivemode());
        }
        return payment;
    }

    public PaymentIntent retrieveStripePaymentIntent(String id) {
        log.info("Retrieving payment intent from Stripe for {}", id);
        Stripe.apiKey = stripeKey;
        try {
            final PaymentIntent paymentIntent = PaymentIntent.retrieve(id);
            if (paymentIntent != null) {
                log.info("Found Stripe payment intent for {}", paymentIntent.getId());
                return paymentIntent;
            } else {
                log.info("Could not find Stripe payment intent for {}", id);
            }
        } catch (StripeException e) {
            log.error("Exception while retrieving stripe intent {}", e.getMessage());
        }
        return null;
    }

    public void deleteAll() {
        paymentRepository.deleteAll();
    }
}
