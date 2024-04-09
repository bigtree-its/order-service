package com.bigtree.order.repository;

import com.bigtree.order.model.FoodOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;

import java.time.LocalDateTime;

@Slf4j
public class FoodOrderMongoListener extends AbstractMongoEventListener<FoodOrder> {

    @Override
    public void onBeforeSave(BeforeSaveEvent<FoodOrder> event) {
        log.info("Setting life cycle fields");
        event.getDocument().put("createdAt", LocalDateTime.now());
        event.getDocument().put("updatedAt", LocalDateTime.now());
    }
}
