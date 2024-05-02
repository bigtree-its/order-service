package com.bigtree.order;

import com.bigtree.order.model.Email;
import com.bigtree.order.service.EmailService;
import com.bigtree.order.service.FoodOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EmailServiceTest {

    @Autowired
    EmailService emailService;
    @Test
    public void test(){
        emailService.sendMail(Email.builder()
                        .to("nava.arul@gmail.com")
                        .subject("hello")
                .build());
    }
}
