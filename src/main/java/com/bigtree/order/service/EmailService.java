package com.bigtree.order.service;

import com.bigtree.order.config.ResourcesConfig;
import com.bigtree.order.helper.EmailContentHelper;
import com.bigtree.order.model.CustomerOrder;
import com.bigtree.order.model.LocalPaymentIntent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Autowired
//    @Qualifier("javaMailSender")
    JavaMailSender javaMailSender;

    @Autowired
    EmailContentHelper emailContentHelper;

    @Autowired
    ResourcesConfig resourcesConfig;

    public void sendOrderConfirmation(CustomerOrder order) {
        log.info("Sending order confirmation customer email {} to {}", order.getReference(), order.getCustomer().getEmail());
        Map<String, Object> params = new HashMap<>();
        params.put("order", order);
        params.put("customer", order.getCustomer());
        params.put("supplier", order.getSupplier());
        params.put("count", order.getItems().size());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        Calendar cal = Calendar.getInstance();
        params.put("today", dateFormat.format(cal.getTime()));
        params.put("deliveryDate", dateFormat.format(cal.getTime()));
        sendMail(order.getCustomer().getEmail(), "#FIRSTBITES Order Confirmation", "order", params);
    }

    public void sendMail(String to, String subject, String template, Map<String, Object> params) {
        final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(emailContentHelper.build(template, params), true);
            javaMailSender.send(mimeMessage);
            log.info("Order confirmation email sent to {}", to);
        } catch (MessagingException e) {
            log.info("Exception while sending confirmation email to {}", to);
        }


    }

    public void sendPaymentLink(CustomerOrder order, LocalPaymentIntent paymentIntent) {
        log.info("Sending payment link to customer {}", order.getCustomer().getEmail());
        final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(order.getCustomer().getEmail());
            helper.setSubject("Accepted. Your Zuvai Order #"+ order.getReference());
            Map<String, Object> params = new HashMap<>();
            params.put("order", order);
            params.put("customer", order.getCustomer());
            params.put("supplier", order.getSupplier());
            params.put("paymentIntentId", paymentIntent.getId());
            helper.setText(emailContentHelper.build("payment-link", params), true);
            javaMailSender.send(mimeMessage);
            log.info("Payment link has been sent to {}", order.getCustomer().getEmail());
        } catch (MessagingException e) {
            log.info("Exception while sending payment link email to {}", order.getCustomer().getEmail());
        }
    }
}
