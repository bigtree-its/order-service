package com.bigtree.order;

import com.bigtree.order.model.Email;
import com.bigtree.order.service.EmailService;
import com.bigtree.order.service.FoodOrderService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
public class EmailServiceTest {

    @Autowired
    JavaMailSender javaMailSender;
    @Test
    public void test(){
        SimpleMailMessage smm =  new SimpleMailMessage();
        smm.setTo("nava.arul@gmail.com");
        smm.setSubject("Test Email");
        smm.setText("Hello");
        javaMailSender.send(smm);
    }
}
