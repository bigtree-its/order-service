package com.bigtree.order.repository;

import com.bigtree.order.model.OrderTracking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderTrackingRepository extends MongoRepository<OrderTracking,String> {

    OrderTracking findFirstByOrderId(String orderId);
    OrderTracking findFirstByReference(String reference);
}
