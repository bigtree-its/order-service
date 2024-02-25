package com.bigtree.order.service;

import com.bigtree.order.exception.ApiException;
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
import org.springframework.http.HttpStatus;
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

    public CustomerOrder createOrder(CustomerOrder order, String action) {
        CustomerOrder response = null;
        if (StringUtils.isEmpty(order.getReference())) {
            String salt = RandomStringUtils.random(6, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
            order.setReference(salt);
            if (action.equalsIgnoreCase("Submit")) {
                order.setStatus(OrderStatus.PENDING);
            } else {
                order.setStatus(OrderStatus.DRAFT);
            }
            order.setDateCreated(LocalDate.now());
            order.setCreatedAt(LocalDateTime.now());
            response = customerOrderRepository.save(order);
            log.info("Saved new order: {}, Ref: {}", response.get_id(), response.getReference());
        } else {
            CustomerOrder loaded = customerOrderRepository.findByReference(order.getReference());
            if (loaded != null) {
                log.info("Order {} already exist. Updating", loaded.getReference());
                response = updateOrder(order, loaded);
            } else {
                log.error("Unable to update order with reference {}. Order not found", order.getReference());
            }
        }
        return response;
    }

    private CustomerOrder updateOrder(CustomerOrder order, CustomerOrder loaded, String action) {
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
        if (action.equalsIgnoreCase("Submit")) {
            loaded.setStatus(OrderStatus.PENDING);
        }
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
                result.add(customerOrderRepository.findByReference(v));
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
                CustomerOrder order = customerOrderRepository.findByReference(intent.getOrderReference());
                if (order != null) {
                    log.info("Found an order with reference {}", intent.getOrderReference());
                }
                result.add(order);
                return result;
            }
        }

        if (StringUtils.isNotEmpty(reference)) {
            CustomerOrder order = customerOrderRepository.findByReference(reference);
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
            CustomerOrder order = customerOrderRepository.findByReference(byIntentId.getOrderReference());
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
                return order;
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

    private CustomerOrder findByReference(String reference) {
        return customerOrderRepository.findByReference(reference);
    }

    private CustomerOrder findByPaymentIntentId(String paymentIntentId) {
        LocalPaymentIntent paymentIntent = paymentRepository.findFirstByIntentId(paymentIntentId);
        if (paymentIntent != null) {
            return findByReference(paymentIntent.getOrderReference());
        }
        log.error("Payment intent not found with id {}", paymentIntentId);
        return null;
    }


    public CustomerOrder update(OrderUpdateRequest request) {
        if (StringUtils.isEmpty(request.getReference()) && StringUtils.isEmpty(request.getPaymentIntentId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Either order reference or payment intent id is mandatory");
        }
        if (StringUtils.isNotEmpty(request.getPaymentIntentId()) && StringUtils.isNotEmpty(request.getPaymentStatus())) {
            return updateStatus(request.getPaymentIntentId(), request.getPaymentStatus());
        }
        CustomerOrder order = null;
        if (StringUtils.isNotEmpty(request.getReference())) {
            order = findByReference(request.getReference());
        }
        if (order == null && StringUtils.isNotEmpty(request.getPaymentIntentId())) {
            order = findByPaymentIntentId(request.getPaymentIntentId());
        }
        if (order == null) {
            log.error("Cannot find an order");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Either valid order reference or payment intent id is mandatory");
        }
        if (StringUtils.isNotEmpty(request.getChefNotes())) {
            order.setNotes(request.getChefNotes());
        }
        if (StringUtils.isNotEmpty(request.getCustomerComments())) {
            order.setCustomerComment(request.getCustomerComments());
        }
        if (request.getCustomerRating() != null) {
            order.setCustomerRating(request.getCustomerRating());
        }
        if (request.getExpectedCollectionDate() != null) {
            order.setCollectBy(request.getExpectedCollectionDate());
        }
        if (request.getExpectedDeliveryDate() != null) {
            order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        }
        log.info("Order updated {}", order.getReference());
        return customerOrderRepository.save(order);
    }

    public CustomerOrder action(String reference, String action) {
        if (StringUtils.isEmpty(reference) || StringUtils.isEmpty(action)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Order Reference and action are mandatory");
        }
        CustomerOrder byReference = customerOrderRepository.findByReference(reference);
        if (byReference == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Order not found");
        }
        switch (action) {
            case "Accept" -> byReference.setStatus(OrderStatus.ACCEPTED);
            case "Submit" -> byReference.setStatus(OrderStatus.PENDING);
            case "Cancel" -> byReference.setStatus(OrderStatus.CANCELLED);
            case "Reject" -> byReference.setStatus(OrderStatus.REJECTED);
            case "Refund" -> byReference.setStatus(OrderStatus.REFUNDED);
            case "Delete" -> deleteOrder(byReference);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Action not supported");
        }
        if (!StringUtils.equalsIgnoreCase("Delete", action)) {
            customerOrderRepository.save(byReference);
            log.info("Order {} status updated to {}", reference, byReference.getStatus().name());
        }
        return byReference;
    }

    private void deleteOrder(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.DRAFT || order.getStatus() == OrderStatus.CANCELLED) {
            customerOrderRepository.delete(order);
            log.info("Order {} has been deleted", order.getReference());
        }
    }

}
