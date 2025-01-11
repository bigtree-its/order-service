package com.bigtree.order;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Properties;

@SpringBootTest
public class EmailServiceTest {

    @Autowired
    JavaMailSender javaMailSender;
    @Test
    public void test() throws MessagingException {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        gmail(mailSender);
//        godaddy(mailSender);
//        ionos(mailSender);
    }

    private static void gmail(JavaMailSenderImpl mailSender) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo("nava.arul@gmail.com");
            helper.setSubject("Test");
            helper.setText("Hello");
            helper.setFrom("orders@dishome.co.uk");

        } catch (MessagingException e) {
           throw e;
        }


        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername("contact.dishome@gmail.com");
        mailSender.setPassword("dsuphdeopyvgdscr");

        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.smtp.starttls.enable", "true");
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.transport.protocol", "smtp");
        javaMailProperties.put("mail.debug", "true");
        javaMailProperties.put("mail.server.protocol", "smtps");
        javaMailProperties.put("mail.smtps.quitwait", "false");
        javaMailProperties.put("mail.smtp.ssl.enable", "false");
        javaMailProperties.put("mail.smtp.ssl.trust", "*");

        mailSender.setJavaMailProperties(javaMailProperties);
        mailSender.send(mimeMessage);
    }

    private static void godaddy(JavaMailSenderImpl mailSender) {
        mailSender.setHost("smtp.office365.com");
        mailSender.setPort(587);
        mailSender.setUsername("support@okeat.co.uk");
        mailSender.setPassword("dflyxkjvtfypkssc");

        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.smtp.starttls.enable", "true");
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.transport.protocol", "smtps");
        javaMailProperties.put("mail.debug", "true");
        javaMailProperties.put("mail.smtp.ssl.enable", "false");
        javaMailProperties.put("mail.smtp.ssl.trust", "*");

        mailSender.setJavaMailProperties(javaMailProperties);
    }

    private static void ionos(JavaMailSenderImpl mailSender) {
        mailSender.setHost("smtp.ionos.co.uk");
        mailSender.setPort(587);
        mailSender.setUsername("contact@pogogi.co.uk");
        mailSender.setPassword("BehindW@@ds123");

        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.smtp.starttls.enable", "true");
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.transport.protocol", "smtp");
        javaMailProperties.put("mail.debug", "true");
        javaMailProperties.put("mail.server.protocol", "smtps");
        javaMailProperties.put("mail.smtps.quitwait", "false");
        javaMailProperties.put("mail.smtp.ssl.enable", "false");
        javaMailProperties.put("mail.smtp.ssl.trust", "*");

        mailSender.setJavaMailProperties(javaMailProperties);
    }
}
