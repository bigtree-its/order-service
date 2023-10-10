package com.bigtree.beku.controller;

import com.bigtree.beku.model.OrderTracking;
import com.bigtree.beku.repository.OrderTrackingRepository;
import com.bigtree.beku.model.CustomerOrder;
import com.bigtree.beku.repository.CustomerOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@Slf4j
@RequestMapping("/api/order-tracking")
@CrossOrigin(origins = "*")
public class OrderTrackingController {

    @Autowired
    OrderTrackingRepository repository;

    @Autowired
    CustomerOrderRepository customerOrderRepository;

    @GetMapping("/reference/{reference}")
    public ResponseEntity<OrderTracking> getStatus(@PathVariable String reference) {
        log.info("Get OrderTracking by reference {}", reference);
        final OrderTracking byOrderReference = repository.findFirstByReference(reference);
        log.info("Returning order tracking by reference {}", reference);
        return ResponseEntity.ok(byOrderReference);
    }

    @PostMapping("")
    public ResponseEntity<OrderTracking> updateOrderTracking(@RequestBody OrderTracking orderTracking) {
        log.info("Update OrderTracking by reference {}", orderTracking.getReference());
        OrderTracking result = null;
        CustomerOrder order = customerOrderRepository.findFirstByReference(orderTracking.getReference());
        if (order != null) {
            OrderTracking byOrderReference = repository.findFirstByReference(orderTracking.getReference());
            if ( byOrderReference == null){
                byOrderReference = OrderTracking.builder()
                        .reference(orderTracking.getReference())
                        .build();
            }
            byOrderReference.setStatus(orderTracking.getStatus());
            byOrderReference.setOrderId(order.get_id());
            switch (orderTracking.getStatus()) {
                case ACCEPTED -> byOrderReference.setDateAccepted(LocalDateTime.now());
                case PAID -> byOrderReference.setDatePaid(LocalDateTime.now());
                case COLLECTED -> byOrderReference.setDateCollected(LocalDateTime.now());
                case CANCELLED -> byOrderReference.setDateCancelled(LocalDateTime.now());
                case DELIVERED -> byOrderReference.setDateDelivered(LocalDateTime.now());
                case REFUNDED -> byOrderReference.setDateRefunded(LocalDateTime.now());
            }
            result = repository.save(byOrderReference);
            log.info("Order status updated {}", result);
            log.info("Order {} status updated to {}", order.getReference(), result.getStatus());
            order.setStatus(orderTracking.getStatus());
            order.setUpdatedAt(LocalDateTime.now());
            customerOrderRepository.save(order);
        }else{
            log.info("Order not found with ref: {}", orderTracking.getReference());
        }
        return ResponseEntity.ok(result);
    }
}
