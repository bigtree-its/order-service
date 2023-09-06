package com.bigtree.beku.service;

import com.bigtree.beku.model.CustomerOrder;
import com.bigtree.beku.model.OrderStatus;
import com.bigtree.beku.model.PaymentIntentRequest;
import com.bigtree.beku.repository.PaymentRepository;
import com.bigtree.beku.repository.CustomerOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    CustomerOrderRepository customerOrderRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    StripeService stripeService;

    public CustomerOrder createOrder(CustomerOrder order) {
        CustomerOrder response = null;
        if (StringUtils.isEmpty(order.getReference())) {
            String salt = RandomStringUtils.random(8, "ABCD1234");
            order.setReference(salt);
            order.setStatus(OrderStatus.CREATED);
            order.setDateCreated(LocalDateTime.now());
            order.setCreatedAt(LocalDateTime.now());
            response = customerOrderRepository.save(order);
            log.info("Saved new order: {}", response.getReference());
        } else {
            CustomerOrder loaded = customerOrderRepository.findFirstByReference(order.getReference());
            if (loaded != null) {
                response = updateOrder(order, loaded);
            } else {
                log.error("Unable to update order with reference {}. Order not found", order.getReference());
            }
        }
        return response;
    }

    private CustomerOrder updateOrder(CustomerOrder order, CustomerOrder loaded) {
        loaded.setUpdatedAt(LocalDateTime.now());
        loaded.setItems(order.getItems());
        loaded.setCustomer(order.getCustomer());
        loaded.setSubTotal(order.getSubTotal());
        loaded.setServiceFee(order.getServiceFee());
        loaded.setPackingFee(order.getPackingFee());
        loaded.setDeliveryFee(order.getDeliveryFee());
        loaded.setStatus(order.getStatus());
        loaded.setNotes(order.getNotes());
        loaded.setServiceMode(order.getServiceMode());
        loaded.setTotal(order.getTotal());
        CustomerOrder updated = customerOrderRepository.save(loaded);
        log.info("Updated order: {}", updated.getReference());
        updatePaymentIntent(updated);
        return updated;
    }

    private void updatePaymentIntent(CustomerOrder order) {
        stripeService.createPaymentIntent(PaymentIntentRequest.builder()
                .customerEmail(order.getCustomer().getEmail())
                .amount(order.getTotal())
                .currency(order.getCurrency())
                .orderReference(order.getReference())
                .build());
    }


    public List<CustomerOrder> findOrdersWithQuery(Map<String, String> qParams) {
        final List<CustomerOrder> result = new ArrayList<>();
        qParams.forEach((k, v) -> {
            if (k.equalsIgnoreCase("customerEmail")) {
                log.info("Looking for orders with customerEmail {}", v);
                result.addAll(customerOrderRepository.findByCustomerEmail(v));
            } else if (k.equalsIgnoreCase("reference")) {
                log.info("Looking for orders with reference {}", v);
                result.add(customerOrderRepository.findFirstByReference(v));
            } else if (k.equalsIgnoreCase("status")) {
                log.info("Looking for orders with status {}", v);
                result.addAll(customerOrderRepository.findByStatus(OrderStatus.valueOf(v)));
            }

        });
        return result;
    }

    public List<CustomerOrder> search(String reference, String customer, String supplier, LocalDate dateFrom, LocalDate dateTo) {
        List<CustomerOrder> result = new ArrayList<>();
        Query query = new Query();
        if (StringUtils.isNotEmpty(reference)) {
            CustomerOrder order = customerOrderRepository.findFirstByReference(reference);
            result.add(order);
            return result;
        }
        if (StringUtils.isNotEmpty(customer)) {
            query.addCriteria(Criteria.where("customer.email").is(customer));
        }
        if (StringUtils.isNotEmpty(supplier)) {
            query.addCriteria(Criteria.where("supplier.email").is(supplier));
        }
        if (dateFrom != null && dateTo != null) {
            if (dateFrom.compareTo(dateTo) == 0) {
                query.addCriteria(Criteria.where("dateCreated").is(dateFrom));
            } else if (!dateFrom.isAfter(dateTo)) {
                query.addCriteria(Criteria.where("dateCreated").gt(dateFrom));
                query.addCriteria(Criteria.where("dateCreated").lt(dateTo));
            }
        } else if (dateFrom != null && dateTo == null) {
            query.addCriteria(Criteria.where("dateCreated").gt(dateFrom));
        } else if (dateTo != null && dateFrom == null) {
            query.addCriteria(Criteria.where("dateCreated").lt(dateTo));
        }
        result = mongoTemplate.find(query, CustomerOrder.class);
        return result;
    }
}
