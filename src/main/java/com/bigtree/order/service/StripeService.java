package com.bigtree.order.service;

import com.bigtree.order.model.LocalPaymentIntent;
import com.bigtree.order.model.PaymentIntentRequest;
import com.bigtree.order.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
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

    public LocalPaymentIntent getPaymentIntent(String orderReference) {
        log.info("Retrieving localPaymentIntent for order {}", orderReference);
        final LocalPaymentIntent localPaymentIntent = paymentRepository.findFirstByOrderReference(orderReference);
        return localPaymentIntent;
    }

    public LocalPaymentIntent createPaymentIntent(PaymentIntentRequest request) {
        Stripe.apiKey = stripeKey;

        final LocalPaymentIntent loadedLocalPaymentIntent = paymentRepository.findFirstByOrderReference(request.getOrderReference());
        BigDecimal reqAmount = request.getAmount().multiply(hundred);
        if (loadedLocalPaymentIntent != null && loadedLocalPaymentIntent.getAmount().compareTo(reqAmount) != 0) {
            log.info("Payment Intent already found, but amount changed for {}", request.getOrderReference());
            return updateStripePaymentIntent(request, loadedLocalPaymentIntent);
        }
        if (loadedLocalPaymentIntent != null && loadedLocalPaymentIntent.getAmount().compareTo(reqAmount) == 0) {
            log.info("Payment Intent already found, no update required for {}", request.getOrderReference());
            return loadedLocalPaymentIntent;
        }
        log.info("Payment Intent not exist for order {}", request.getOrderReference());
        final BigDecimal stripeAmount = request.getAmount().multiply(hundred);

        // Create new Payment Intent
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
            return saveNewLocalPaymentIntent(request, created);
        } catch (StripeException e) {
            log.error("Unable to create payment intent {}", e.getMessage());
            return LocalPaymentIntent.builder()
                    .error(true)
                    .errorMessage(e.getMessage())
                    .build();
        }

    }

    public LocalPaymentIntent updateStripePaymentIntent(PaymentIntentRequest request, LocalPaymentIntent loadedLocalPaymentIntent) {
        LocalPaymentIntent updatedLocalPaymentIntent = null;
        try {
            final BigDecimal stripeAmount = request.getAmount().multiply(hundred);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("amount", stripeAmount.longValue());
            Map<String, Object> params = new HashMap<>();
            params.put("metadata", metadata);
            params.put("amount", stripeAmount.longValue());
            final PaymentIntent paymentIntent = PaymentIntent.retrieve(loadedLocalPaymentIntent.getIntentId());
            if ( paymentIntent.getStatus().equalsIgnoreCase("succeeded")){
                log.error("Existing intent for this order reference {} is succeeded. Cannot update anymore.", request.getOrderReference());
                return LocalPaymentIntent.builder()
                        .error(true)
                        .errorMessage("Existing intent for this order reference succeeded. Cannot update.")
                        .build();
            }
            PaymentIntent updated = paymentIntent.update(params);
            log.info("Payment Intent is updated with new amount {} for order {}", stripeAmount, request.getOrderReference());
            updatedLocalPaymentIntent = updateLocalPaymentIntent(request, updated, loadedLocalPaymentIntent);
        } catch (StripeException e) {
            log.error("Error while updating LocalPaymentIntent Intent. {}", e.getMessage());
            return LocalPaymentIntent.builder()
                    .error(true)
                    .errorMessage(e.getMessage())
                    .build();
        }
        return updatedLocalPaymentIntent;
    }

    private LocalPaymentIntent updateLocalPaymentIntent(PaymentIntentRequest request, PaymentIntent stripeIntent, LocalPaymentIntent loadedLocalPaymentIntent) {
        log.info("LocalPaymentIntent already found. Updating for {}", request.getOrderReference());
        loadedLocalPaymentIntent.setAmount(BigDecimal.valueOf(stripeIntent.getAmount()));
        loadedLocalPaymentIntent.setPaymentMethod(stripeIntent.getPaymentMethod());
        loadedLocalPaymentIntent.setStatus(stripeIntent.getStatus());
        loadedLocalPaymentIntent.setCustomer(request.getCustomerEmail());
        final LocalPaymentIntent updatedLocalPaymentIntent = paymentRepository.save(loadedLocalPaymentIntent);
        log.info("LocalPaymentIntent is updated for {} ", updatedLocalPaymentIntent.getOrderReference());
        return updatedLocalPaymentIntent;
    }

    private LocalPaymentIntent saveNewLocalPaymentIntent(PaymentIntentRequest request, PaymentIntent stripeIntent) {
        final LocalPaymentIntent newLocalPaymentIntent = paymentRepository.save(LocalPaymentIntent.builder()
                .intentId(stripeIntent.getId())
                .orderReference(request.getOrderReference())
                .customer(request.getCustomerEmail())
                .amount(BigDecimal.valueOf(stripeIntent.getAmount()))
                .paymentMethod(stripeIntent.getPaymentMethodTypes().get(0))
                .clientSecret(stripeIntent.getClientSecret())
                .currency(stripeIntent.getCurrency())
                .status(stripeIntent.getStatus())
                .supplier(request.getSupplierId())
                .build());
        if (newLocalPaymentIntent != null) {
            log.info("New LocalPaymentIntent is saved for order {}" , newLocalPaymentIntent.getOrderReference());
        }
        return newLocalPaymentIntent;
    }
}
