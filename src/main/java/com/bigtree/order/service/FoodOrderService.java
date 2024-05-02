package com.bigtree.order.service;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.model.*;
import com.bigtree.order.repository.PaymentRepository;
import com.bigtree.order.repository.FoodOrderRepository;
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
public class FoodOrderService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    FoodOrderRepository customerOrderRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    StripeService stripeService;

    @Autowired
    EmailService emailService;

    public FoodOrder createOrder(FoodOrder order, String action) {
        FoodOrder foodOrder = null;
        if (StringUtils.isEmpty(order.getReference())) {
            String salt1 = RandomStringUtils.random(3, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
            String salt2 = RandomStringUtils.random(3, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
            String salt3 = RandomStringUtils.random(3, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
            order.setReference(salt1 + "-" + salt2 + "-" + salt3);
            order.setStatus(OrderStatus.Draft);
            order.setDateCreated(LocalDate.now());
            order.setCreatedAt(LocalDateTime.now());
            foodOrder = customerOrderRepository.save(order);
            log.info("Created new order: {}, Ref: {}", foodOrder.get_id(), foodOrder.getReference());
            if (StringUtils.isNotEmpty(action)) {
                foodOrder = action(foodOrder.getReference(), action);
            }
//            else {
//                final Map<String, Object> params = buildEmailParams(response);
//                final Email email = buildEmail(response, params);
//                emailService.sendMail(email);
//            }
        } else {
            FoodOrder loaded = customerOrderRepository.findByReference(order.getReference());
            if (loaded != null) {
                log.info("Order {} already exist. Updating", loaded.getReference());
                foodOrder = updateOrder(order, loaded, action);
            } else {
                log.error("Unable to update order with reference {}. Order not found", order.getReference());
            }
        }
        return foodOrder;
    }

    private FoodOrder updateOrder(FoodOrder order, FoodOrder loaded, String action) {
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
        FoodOrder updated = customerOrderRepository.save(loaded);
        log.info("Updated order: {}", updated.getReference());
        if (StringUtils.isNotEmpty(action)) {
            action(updated.getReference(), action);
        }
        return updated;
    }

    public List<FoodOrder> search(String intentId, String reference, String customer, String supplier,
                                  LocalDate date, LocalDate dateFrom, LocalDate dateTo) {
        List<FoodOrder> result = new ArrayList<>();
        Query query = new Query();
        if (StringUtils.isNotEmpty(intentId)) {
            log.info("Searching order with payment intent {}", intentId);
            Payment intent = paymentRepository.findFirstByIntentId(intentId);
            if (intent != null) {
                FoodOrder order = customerOrderRepository.findByReference(intent.getOrderReference());
                if (order != null) {
                    log.info("Found an order with reference {}", intent.getOrderReference());
                }
                result.add(order);
                return result;
            }
        }

        if (StringUtils.isNotEmpty(reference)) {
            FoodOrder order = customerOrderRepository.findByReference(reference);
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
        result = mongoTemplate.find(query, FoodOrder.class);
        return result;
    }


    private FoodOrder findByReference(String reference) {
        return customerOrderRepository.findByReference(reference);
    }

    private FoodOrder findByPaymentIntentId(String paymentIntentId) {
        Payment paymentIntent = paymentRepository.findFirstByIntentId(paymentIntentId);
        if (paymentIntent != null) {
            return findByReference(paymentIntent.getOrderReference());
        }
        log.error("Payment intent not found with id {}", paymentIntentId);
        return null;
    }


    public FoodOrder update(OrderUpdateRequest request) {
        if (StringUtils.isEmpty(request.getReference()) && StringUtils.isEmpty(request.getPaymentIntentId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Either order reference or payment intent id is mandatory");
        }
        FoodOrder order = null;
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
        if (request.getStatus() != null) {
            order.setStatus(OrderStatus.valueOf(request.getStatus()));
        }
        if (StringUtils.isNotEmpty(request.getPaymentStatus())) {
            if (request.getPaymentStatus().equalsIgnoreCase("succeeded")) {
                payment(order);
            }
        }
        log.info("Order updated {}. Status {}", order.getReference(), order.getStatus());
        return customerOrderRepository.save(order);
    }

    public FoodOrder action(String reference, String action) {
        if (StringUtils.isEmpty(reference) || StringUtils.isEmpty(action)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Order Reference and action are mandatory");
        }
        FoodOrder foodOrder = customerOrderRepository.findByReference(reference);
        if (foodOrder == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Order not found");
        }
        switch (action) {
            case "Accept" -> acceptOrder(foodOrder);
            case "Submit" -> submitOrder(foodOrder);
            case "Cancel" -> cancelOrder(foodOrder);
            case "Reject" -> rejectOrder(foodOrder);
            case "Ready" -> {
                foodOrder.setStatus(OrderStatus.Ready);
            }
            case "OutForDelivery" -> {
                foodOrder.setStatus(OrderStatus.OutForDelivery);
            }
            case "Pay" -> payment(foodOrder);
            case "IntentToPay" -> {
                foodOrder = paymentIntent(foodOrder);
            }
            case "Refund" -> refundOrder(foodOrder);
            case "Delete" -> deleteOrder(foodOrder);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Action not supported");
        }

        return foodOrder;
    }

    public FoodOrder paymentIntent(FoodOrder order) {
        PaymentIntentRequest req = PaymentIntentRequest.builder()
                .amount(order.getTotal())
                .orderReference(order.getReference())
                .currency("GBP")
                .customerEmail(order.getCustomer().getEmail())
                .supplierId(order.getSupplier().get_id())
                .build();
        final PaymentIntent paymentIntent = stripeService.createPaymentIntent(req);
        if (paymentIntent != null) {
            log.info("Created payment intent {} with secret {} for order {}", paymentIntent.getId(), paymentIntent.getClientSecret(), order.getReference());
            order.setPaymentIntentId(paymentIntent.getId());
            order.setClientSecret(paymentIntent.getClientSecret());
            return customerOrderRepository.save(order);
        }
        return order;
    }

    private void refundOrder(FoodOrder order) {
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

    private void payment(FoodOrder order) {
        Payment localIntent = paymentRepository.findFirstByOrderReference(order.getReference());
        if (localIntent != null) {
            final PaymentIntent paymentIntent = stripeService.retrieveStripePaymentIntent(localIntent.getIntentId());
            if (paymentIntent != null) {
                if (paymentIntent.getStatus().equalsIgnoreCase("Succeeded")) {
                    log.info("Order {} is Paid", order.getReference());
                    order.setStatus(OrderStatus.Paid);
                    order.setDatePaid(LocalDateTime.now());
                    final Map<String, Object> params = buildEmailParams(order);
                    final Email email = buildEmail(order, params);
                    Runnable task = () -> {
                        emailService.sendMail(email);
                    };
                    Thread thread = new Thread(task);
                    thread.start();
                }
            }
        }
    }

    private void cancelOrder(FoodOrder order) {
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

    private void rejectOrder(FoodOrder order) {
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

    private void submitOrder(FoodOrder order) {
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

    private void acceptOrder(FoodOrder order) {
        if (order.getStatus() == OrderStatus.Pending) {
            order.setStatus(OrderStatus.Accepted);
            order.setDateAccepted(LocalDateTime.now());
            log.info("Order {} is accepted by Chef {}", order.getReference(), order.getSupplier().get_id());
            PaymentIntent paymentIntent = stripeService.createPaymentIntent(PaymentIntentRequest.builder()
                    .orderReference(order.getReference())
                    .amount(order.getTotal())
                    .currency(order.getCurrency())
                    .customerEmail(order.getCustomer().getEmail())
                    .supplierId(order.getSupplier().get_id())
                    .build());
            final Map<String, Object> params = buildEmailParams(order);
            params.put("linkUrl", "http://localhost:4200/make_payment?ref=" + order.getReference() + "&intent=" + paymentIntent.getId());
            params.put("linkText", "Make Payment");
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot accepted at this stage. {}", order.getReference(), order.getStatus().name());
        }
    }

    private Email buildEmail(FoodOrder order, Map<String, Object> params) {
        final Email email = Email.builder()
                .to(order.getCustomer().getEmail())
                .subject(order.getStatus() + ": Your DESILAND order " + order.getReference())
                .params(params)
                .build();
        return email;
    }

    private Map<String, Object> buildEmailParams(FoodOrder order) {
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

    private String emailMessage(FoodOrder order) {
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
            case InProgress -> {
                message = "Your order is in progress";
            }
            case Ready -> {
                if (order.getServiceMode() == ServiceMode.COLLECTION) {
                    message = "Your order is Ready for collection";
                } else {
                    message = "Your order is Ready for Delivery";
                }
            }
            case OutForDelivery -> {
                if (order.getServiceMode() == ServiceMode.COLLECTION) {
                    message = "Your order is Ready for collection";
                } else {
                    message = "Your order is Ready for Out for Delivery";
                }
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

    private void deleteOrder(FoodOrder order) {
        if (order.getStatus() == OrderStatus.Draft || order.getStatus() == OrderStatus.Cancelled) {
            customerOrderRepository.delete(order);
            log.info("Order {} has been deleted", order.getReference());
        } else {
            log.info("Order {} cannot be deleted at this point {}", order.getReference(), order.getStatus().name());
        }
    }

    public void deleteByRef(String ref) {
        final FoodOrder byReference = customerOrderRepository.findByReference(ref);
        if (byReference != null) {
            deleteOrder(byReference);
        }
    }
}
