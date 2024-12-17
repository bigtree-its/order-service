package com.bigtree.order.service;

import com.bigtree.order.config.ResourcesConfig;
import com.bigtree.order.helper.EmailContentHelper;
import com.bigtree.order.model.Email;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

    public void sendMail(Email email) {
        log.info("Sending email to customer {}", email.getTo());
        final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(email.getTo());
            helper.setSubject(email.getSubject());
            helper.setFrom("orders@eatem.co.uk");
            helper.setText(emailContentHelper.build("order", email.getParams()), true);
            javaMailSender.send(mimeMessage);
            log.info("Email sent to customer {}", email.getTo());
        } catch (MessagingException e) {
            log.info("Exception while sending payment link email to {}", email.getTo());
        } catch (Exception e){
            log.error("Error while sending email to {}", email.getTo(), e);
        }
    }
}
