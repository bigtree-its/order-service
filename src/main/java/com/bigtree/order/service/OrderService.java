package com.bigtree.order.service;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.model.*;
import com.bigtree.order.repository.PaymentRepository;
import com.bigtree.order.repository.CustomerOrderRepository;
import com.stripe.model.PaymentIntent;
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
            order.setStatus(OrderStatus.Draft);
            order.setDateCreated(LocalDate.now());
            order.setCreatedAt(LocalDateTime.now());
            response = customerOrderRepository.save(order);
            log.info("Saved new order: {}, Ref: {}", response.get_id(), response.getReference());
            if (StringUtils.isNotEmpty(action)) {
                action(response.getReference(), action);
            } else {
                final Map<String, Object> params = buildEmailParams(response);
                final Email email = buildEmail(response, params);
                emailService.sendMail(email);
            }
        } else {
            CustomerOrder loaded = customerOrderRepository.findByReference(order.getReference());
            if (loaded != null) {
                log.info("Order {} already exist. Updating", loaded.getReference());
                response = updateOrder(order, loaded, action);
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
        CustomerOrder updated = customerOrderRepository.save(loaded);
        log.info("Updated order: {}", updated.getReference());
        if (StringUtils.isNotEmpty(action)) {
            action(updated.getReference(), action);
        }
        return updated;
    }

      public List<CustomerOrder> search(String intentId, String reference, String customer, String supplier,
                                      LocalDate date, LocalDate dateFrom, LocalDate dateTo) {
        List<CustomerOrder> result = new ArrayList<>();
        Query query = new Query();
        if (StringUtils.isNotEmpty(intentId)) {
            log.info("Searching order with payment intent {}", intentId);
            Payment intent = paymentRepository.findFirstByIntentId(intentId);
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
            query.addCriteria(Criteria.where("supplier._id").is(supplier));
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


    private CustomerOrder findByReference(String reference) {
        return customerOrderRepository.findByReference(reference);
    }

    private CustomerOrder findByPaymentIntentId(String paymentIntentId) {
        Payment paymentIntent = paymentRepository.findFirstByIntentId(paymentIntentId);
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
        CustomerOrder order = null;
        if (StringUtils.isNotEmpty(request.getReference())) {
            order = findByReference(request.getReference());
        }
        if (order == null && StringUtils.isNotEmpty(request.getPaymentIntentId())) {
            order = findByPaymentIntentId(request.getPaymentIntentId());
        }
        if (order == null) {
            log.error("Cannot find an order {}", request.getReference());
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
            order.setCollectionDate(request.getExpectedCollectionDate());
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
            case "Accept" -> acceptOrder(byReference);
            case "Submit" -> submitOrder(byReference);
            case "Cancel" -> cancelOrder(byReference);
            case "Reject" -> rejectOrder(byReference);
            case "Pay" -> payment(byReference);
            case "Refund" -> refundOrder(byReference);
            case "Delete" -> deleteOrder(byReference);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Action not supported");
        }
        if (!StringUtils.equalsIgnoreCase("Delete", action)) {
            customerOrderRepository.save(byReference);
            log.info("Order {} status {}", reference, byReference.getStatus().name());
        }
        return byReference;
    }

    private void refundOrder(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.Paid) {
            order.setStatus(OrderStatus.Cancelled);
            order.setDateRefunded(LocalDateTime.now());
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be cancelled at this stage. {}", order.getReference(), order.getStatus().name());
        }
    }

    private void payment(CustomerOrder order) {
        Payment localIntent = paymentRepository.findFirstByOrderReference(order.getReference());
        if (localIntent != null) {
            final PaymentIntent paymentIntent = stripeService.retrieveStripePaymentIntent(localIntent.getIntentId());
            if (paymentIntent != null) {
                if (paymentIntent.getStatus().equalsIgnoreCase("Succeeded")) {
                    order.setStatus(OrderStatus.Paid);
                    order.setDatePaid(LocalDateTime.now());
                    final Map<String, Object> params = buildEmailParams(order);
                    final Email email = buildEmail(order, params);
                    emailService.sendMail(email);
                }
            }
        }
    }

    private void cancelOrder(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.Pending) {
            order.setStatus(OrderStatus.Cancelled);
            order.setDateCancelled(LocalDateTime.now());
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be cancelled at this stage. {}", order.getReference(), order.getStatus().name());
        }
    }

    private void rejectOrder(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.Pending) {
            order.setStatus(OrderStatus.Rejected);
            order.setDateRejected(LocalDateTime.now());
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be rejected at this stage. {}", order.getReference(), order.getStatus().name());
        }
    }

    private void submitOrder(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.Draft) {
            order.setStatus(OrderStatus.Pending);
            order.setDateSubmitted(LocalDateTime.now());
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be submitted at this stage. {}", order.getReference(), order.getStatus().name());
        }
    }

    private void acceptOrder(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.Pending) {
            order.setStatus(OrderStatus.Accepted);
            order.setDateAccepted(LocalDateTime.now());
            log.info("Order {} is accepted by Chef {}", order.getReference(), order.getSupplier().get_id());
            Payment paymentIntent = stripeService.createPaymentIntent(PaymentIntentRequest.builder()
                    .orderReference(order.getReference())
                    .amount(order.getTotal())
                    .currency(order.getCurrency())
                    .customerEmail(order.getCustomer().getEmail())
                    .supplierId(order.getSupplier().get_id())
                    .build());
            final Map<String, Object> params = buildEmailParams(order);
            params.put("linkUrl", "http://localhost:4200/make_payment?ref=" + order.getReference() + "&intent=" + paymentIntent.getIntentId());
            params.put("linkText", "Make Payment");
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot accepted at this stage. {}", order.getReference(), order.getStatus().name());
        }
    }

    private Email buildEmail(CustomerOrder order, Map<String, Object> params) {
        final Email email = Email.builder()
                .to(order.getCustomer().getEmail())
                .subject(order.getStatus() + ": Your Zuvai order " + order.getReference())
                .params(params)
                .build();
        return email;
    }

    private Map<String, Object> buildEmailParams(CustomerOrder order) {
        Map<String, Object> params = new HashMap<>();
        params.put("message", emailMessage(order));
        params.put("order", order);
        params.put("status", order.getStatus().name());
        params.put("serviceMode", order.getServiceMode().name());
        params.put("customer", order.getCustomer());
        params.put("items", order.getItems());
        params.put("supplier", order.getSupplier());
        return params;
    }

    private String emailMessage(CustomerOrder order) {
        String message = "";
        switch (order.getStatus()) {
            case Draft -> {
                message = "Your order still in Draft state. Please submit to chef.";
            }
            case Created -> {
            }
            case Paid -> {
                message = "Your order has been Paid. We will notify you once your order prepared by Chef.";
            }
            case In_Progress -> {
                message = "Your order is in progress";
            }
            case Pending -> {
                message = "Your order has been submitted to Chef and waiting to be accepted.";
            }
            case Accepted -> {
                message = "Your order has been accepted by Chef. Please make a payment using below link.";
            }
            case Collected -> {
                message = "Your have collected your order.";
            }
            case Cancelled -> {
                message = "Your order has been cancelled.";
            }
            case Refunded -> {
                message = "Your order has been refunded.";
            }
            case Delivered -> {
                message = "Your order has been delivered.";
            }
            case Rejected -> {
                message = "Sorry the chef is unable to accept your order at this point. Please see the notes from Chef.";
            }
            case Payment_Error -> {
                message = "Sorry there was an payment error for your order. Please make a payment.";
            }
        }
        return message;
    }

    private void deleteOrder(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.Draft || order.getStatus() == OrderStatus.Cancelled) {
            customerOrderRepository.delete(order);
            log.info("Order {} has been deleted", order.getReference());
        } else {
            log.info("Order {} cannot be deleted at this point {}", order.getReference(), order.getStatus().name());
        }
    }

    public void deleteByRef(String ref) {
        final CustomerOrder byReference = customerOrderRepository.findByReference(ref);
        if (byReference != null) {
            deleteOrder(byReference);
        }
    }
}
