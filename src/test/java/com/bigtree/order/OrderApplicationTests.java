package com.bigtree.order;

import com.bigtree.order.model.LocalPaymentIntent;
import com.bigtree.order.service.EmailService;
import com.bigtree.order.service.StripeService;
import com.bigtree.order.model.CustomerOrder;
import com.bigtree.order.model.PaymentIntentRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
class OrderApplicationTests {

    @Autowired
    EmailService emailService;

    @Autowired
    StripeService stripeService;

    @Test
    void contextLoads() {
    }

//    @Test
    public void sendEmail() {
        CustomerOrder order = DummyData.createDummyOrder();
        emailService.sendOrderConfirmation(order);
    }

    @Test
    public void testCreatePaymentIntent() {
        BigDecimal amount= new BigDecimal("15.50");
        BigDecimal hundred= new BigDecimal("100");
        BigDecimal stripeAmount= amount.multiply(hundred);
        final LocalPaymentIntent response = stripeService.createPaymentIntent(PaymentIntentRequest.builder()
                .amount(amount)
                .orderReference("64eb982fe021a97a4a922f6abcd")
                .customerEmail("nava.arul@gmail.com")
                .currency("GBP")
                .build());
        Assertions.assertNotNull(response);

        // Do nothing scenario
        final LocalPaymentIntent doNothing = stripeService.createPaymentIntent(PaymentIntentRequest.builder()
                .amount(amount)
                .orderReference("64eb982fe021a97a4a922f6abcd")
                .customerEmail("nava.arul@gmail.com")
                .currency("GBP")
                .build());
        Assertions.assertNotNull(doNothing);
        Assertions.assertEquals("64eb982fe021a97a4a922f6abcd", doNothing.getOrderReference());
        Assertions.assertEquals("nava.arul@gmail.com", doNothing.getCustomer());


        // Amount Changed
        final BigDecimal newAmount = amount.add(BigDecimal.TEN);
        final LocalPaymentIntent updateIntent = stripeService.createPaymentIntent(PaymentIntentRequest.builder()
                .amount(newAmount)
                .orderReference("64eb982fe021a97a4a922f6abcd")
                .customerEmail("nava.arul@gmail.com")
                .currency("GBP")
                .build());
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(updateIntent);
//        Assertions.assertEquals(newAmount.multiply(hundred), updateIntent.getAmount());
        Assertions.assertEquals("64eb982fe021a97a4a922f6abcd", updateIntent.getOrderReference());
        Assertions.assertEquals("nava.arul@gmail.com", updateIntent.getCustomer());
    }
}
