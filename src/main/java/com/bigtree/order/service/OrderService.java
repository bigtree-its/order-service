package com.bigtree.order.service;

import com.bigtree.order.model.*;
import com.bigtree.order.repository.PaymentRepository;
import com.bigtree.order.repository.CustomerOrderRepository;
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
import java.util.HashMap;
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

    @Autowired
    EmailService emailService;

    public CustomerOrder createOrder(CustomerOrder order) {
        CustomerOrder response = null;
        if (StringUtils.isEmpty(order.getReference())) {
            String salt = RandomStringUtils.random(8, "ABCD1234");
            order.setReference(salt);
            order.setStatus(OrderStatus.CREATED);
            order.setDateCreated(LocalDate.now());
            order.setCreatedAt(LocalDateTime.now());
            response = customerOrderRepository.save(order);
            log.info("Saved new order: {}, Ref: {}", response.get_id(), response.getReference());
        } else {
            CustomerOrder loaded = customerOrderRepository.findFirstByReference(order.getReference());
            if (loaded != null) {
                log.info("Order {}, Ref: {} already exist. Updating", loaded.get_id());
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

    public List<CustomerOrder> search(String intentId, String reference, String customer, String supplier,
                                      LocalDate date, LocalDate dateFrom, LocalDate dateTo) {
        List<CustomerOrder> result = new ArrayList<>();
        Query query = new Query();
        if (StringUtils.isNotEmpty(intentId)) {
            log.info("Searching order with payment intent {}", intentId);
            LocalPaymentIntent intent = paymentRepository.findFirstByIntentId(intentId);
            if (intent != null) {
                CustomerOrder order = customerOrderRepository.findFirstByReference(intent.getOrderReference());
                if (order != null) {
                    log.info("Found an order with reference {}", intent.getOrderReference());
                }
                result.add(order);
                return result;
            }
        }

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
        if (date != null) {
            query.addCriteria(Criteria.where("dateCreated").is(date));
        } else {
            if (dateFrom != null && dateTo != null) {
                query.addCriteria(Criteria.where("dateCreated").gte(dateFrom).lte(dateTo));
            } else if (dateTo != null) {
                query.addCriteria(Criteria.where("dateCreated").lte(dateTo));
            } else if (dateFrom != null) {
                query.addCriteria(Criteria.where("dateCreated").gte(dateFrom));
            }
        }

        log.info("Searching orders with query {}", query.toString());
        result = mongoTemplate.find(query, CustomerOrder.class);
        return result;
    }

    public CustomerOrder updateStatus(String paymentIntentId, String status) {
        LocalPaymentIntent byIntentId = paymentRepository.findFirstByIntentId(paymentIntentId);
        if (byIntentId != null) {
            byIntentId.setStatus(status);
            paymentRepository.save(byIntentId);
            CustomerOrder order = customerOrderRepository.findFirstByReference(byIntentId.getOrderReference());
            if (order != null) {
                log.info("Order found with reference {}", order.getReference());
                if (status.equalsIgnoreCase("succeeded")) {
                    order.setStatus(OrderStatus.PAID);
                } else {
                    order.setStatus(OrderStatus.PAYMENT_ERROR);
                }
                order.setUpdatedAt(LocalDateTime.now());
                customerOrderRepository.save(order);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendEmail(order);
                    }
                }).start();
                return  order;
            }
        }
        log.info("Cannot locate payment intent {}", paymentIntentId);
        return null;
    }

    private void sendEmail(CustomerOrder order) {
        String message = "";
        if (order.getStatus() == OrderStatus.PAID) {
            message = "We have received your payment.";
        } else if (order.getStatus() == OrderStatus.PAYMENT_ERROR) {
            message = "There is an issue with your payment";
        }
        String subject = "Your Chumma order " + order.getReference();
        Map<String, Object> body = new HashMap<>();
        body.put("order", order);
        body.put("customer", order.getCustomer());
        body.put("items", order.getItems());
        body.put("message", message);
        body.put("supplier", order.getSupplier());
        emailService.sendMail(order.getCustomer().getEmail(), subject, "order-update", body);
    }

    public CustomerOrder update(OrderUpdateRequest orderUpdateRequest) {
        if (StringUtils.isNotEmpty(orderUpdateRequest.getPaymentIntentId()) && StringUtils.isNotEmpty(orderUpdateRequest.getPaymentStatus())){
            return updateStatus(orderUpdateRequest.getPaymentIntentId(), orderUpdateRequest.getPaymentStatus());
        }
        return null;
    }
}
