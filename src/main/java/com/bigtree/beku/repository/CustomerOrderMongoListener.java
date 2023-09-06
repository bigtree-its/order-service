package com.bigtree.beku.repository;

import com.bigtree.beku.model.CustomerOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;

import java.time.LocalDateTime;

@Slf4j
public class CustomerOrderMongoListener extends AbstractMongoEventListener<CustomerOrder> {

    @Override
    public void onBeforeSave(BeforeSaveEvent<CustomerOrder> event) {
        log.info("Setting life cycle fields");
        event.getDocument().put("createdAt", LocalDateTime.now());
        event.getDocument().put("updatedAt", LocalDateTime.now());
    }
}
