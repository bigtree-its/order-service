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
import org.springframework.util.CollectionUtils;

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
            String salt1 = RandomStringUtils.random(2, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
            String salt2 = RandomStringUtils.random(2, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
            String salt3 = RandomStringUtils.random(2, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
            order.setReference(salt1 + "-" + salt2 + "-" + salt3);
            order.setStatus(OrderStatus.New);
            order.setDateCreated(LocalDate.now());
            order.setCreatedAt(LocalDateTime.now());
            foodOrder = customerOrderRepository.save(order);
            log.info("Created new order: {}, Ref: {}", foodOrder.get_id(), foodOrder.getReference());
            if (StringUtils.isNotEmpty(action)) {
                foodOrder = action(foodOrder.getReference(), action);
            }
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
        loaded.setCustomerNotes(order.getCustomerNotes());
        loaded.setKitchenNotes(order.getKitchenNotes());
        loaded.setServiceMode(order.getServiceMode());
        loaded.setTotal(order.getTotal());
        FoodOrder updated = customerOrderRepository.save(loaded);
        log.info("Updated order: {}", updated.getReference());
        if (StringUtils.isNotEmpty(action)) {
            action(updated.getReference(), action);
        }
        return updated;
    }

    public List<FoodOrder> search(String intentId, String reference, String customer, String cloudKitchenId,
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
        if (StringUtils.isNotEmpty(cloudKitchenId)) {
            query.addCriteria(Criteria.where("cloudKitchen._id").is(cloudKitchenId));
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
        if (!CollectionUtils.isEmpty(request.getKitchenNotes())) {
            order.setKitchenNotes(request.getKitchenNotes());
        }
        if (!CollectionUtils.isEmpty(request.getCustomerNotes())) {
            order.setCustomerNotes(request.getCustomerNotes());
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
            order.setStatus(request.getStatus());
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
            case "Pickup" -> pickup(foodOrder);
            case "Cancel" -> {
                if (foodOrder.getStatus().equalsIgnoreCase(OrderStatus.New)) {
                    deleteOrder(foodOrder);
                } if (foodOrder.getStatus().equalsIgnoreCase(OrderStatus.Paid)) {
                    cancelOrder(foodOrder);
                }
            }
            case "Decline" -> declineOrder(foodOrder);
            case "Ready" -> {
                readyOrder(foodOrder);
            }
            case "Pay" -> payment(foodOrder);
            case "IntentToPay" -> {
                foodOrder = paymentIntent(foodOrder);
            }
            case "Complete Refund" -> refundOrder(foodOrder);
            case "Start Refund" -> startRefundOrder(foodOrder);
            case "Collected" -> orderCollected(foodOrder);
            case "Delivered" -> orderDelivered(foodOrder);
            case "Out for delivery" -> orderOutForDelivery(foodOrder);
            case "Delete" -> deleteOrder(foodOrder);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", "Action not supported");
        }

        return foodOrder;
    }

    private void readyOrder(FoodOrder order) {
        if (order.getStatus().equalsIgnoreCase(OrderStatus.In_Progress)) {
            order.setStatus(OrderStatus.Ready);
            order.setDateReady(LocalDateTime.now());
            customerOrderRepository.save(order);
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be set as Ready at this stage {}", order.getReference(), order.getStatus());
        }
    }
    private void orderCollected(FoodOrder order) {
        if (order.getStatus().equalsIgnoreCase(OrderStatus.Ready)) {
            order.setStatus(OrderStatus.Collected);
            order.setDateReady(LocalDateTime.now());
            customerOrderRepository.save(order);
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be set as collected at this stage {}", order.getReference(), order.getStatus());
        }
    }

    private void orderOutForDelivery(FoodOrder order) {
        if (order.getStatus().equalsIgnoreCase(OrderStatus.Ready)) {
            order.setStatus(OrderStatus.Out_For_Delivery);
            order.setDateReady(LocalDateTime.now());
            customerOrderRepository.save(order);
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be set as out for delivery at this stage {}", order.getReference(), order.getStatus());
        }
    }

    private void orderDelivered(FoodOrder order) {
        if (order.getStatus().equalsIgnoreCase(OrderStatus.Ready)) {
            order.setStatus(OrderStatus.Delivered);
            order.setDateReady(LocalDateTime.now());
            customerOrderRepository.save(order);
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be set delivered at this stage {}", order.getReference(), order.getStatus());
        }
    }

    public FoodOrder paymentIntent(FoodOrder order) {
        PaymentIntentRequest req = PaymentIntentRequest.builder()
                .amount(order.getTotal())
                .orderReference(order.getReference())
                .currency("GBP")
                .customerEmail(order.getCustomer().getEmail())
                .cloudKitchenId(order.getCloudKitchen().get_id())
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

    private void startRefundOrder(FoodOrder order) {
        if (order.getStatus().equalsIgnoreCase(OrderStatus.Declined)) {
            order.setStatus(OrderStatus.Refund_Started);
            order.setDateRefundStarted(LocalDateTime.now());
            customerOrderRepository.save(order);
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Refund for order {} cannot be started at this stage. {}", order.getReference(), order.getStatus());
        }
    }

    private void refundOrder(FoodOrder order) {
        if (order.getStatus().equalsIgnoreCase(OrderStatus.Refund_Started)) {
            order.setStatus(OrderStatus.Refunded);
            order.setDateRefunded(LocalDateTime.now());
            customerOrderRepository.save(order);
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be refunded at this stage. {}", order.getReference(), order.getStatus());
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
                    customerOrderRepository.save(order);
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
        if (order.getStatus().equalsIgnoreCase(OrderStatus.Paid)) {
            order.setStatus(OrderStatus.Cancelled);
            order.setDateCancelled(LocalDateTime.now());
            customerOrderRepository.save(order);
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be cancelled at this stage. {}", order.getReference(), order.getStatus());
        }
    }

    private void declineOrder(FoodOrder order) {
        if (order.getStatus().equalsIgnoreCase(OrderStatus.Paid)  || order.getStatus().equalsIgnoreCase(OrderStatus.New)) {
            order.setStatus(OrderStatus.Declined);
            order.setDateRejected(LocalDateTime.now());
            customerOrderRepository.save(order);
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot be declined at this stage. {}", order.getReference(), order.getStatus());
        }
    }

    private void pickup(FoodOrder order) {
        if (order.getStatus().equalsIgnoreCase(OrderStatus.Paid)) {
            order.setStatus(OrderStatus.In_Progress);
            order.setKitchenAction("Pickup");
            order.setDateAccepted(LocalDateTime.now());
            customerOrderRepository.save(order);
            log.info("Order {} is picked up by kitchen {}", order.getReference(), order.getCloudKitchen().get_id());
            final Map<String, Object> params = buildEmailParams(order);
            final Email email = buildEmail(order, params);
            emailService.sendMail(email);
        } else {
            log.error("Order {} cannot picked up at this stage. {}", order.getReference(), order.getStatus());
        }
    }

    private Email buildEmail(FoodOrder order, Map<String, Object> params) {
        final Email email = Email.builder()
                .to(order.getCustomer().getEmail())
                .subject("Your order " + order.getReference())
                .params(params)
                .build();
        return email;
    }

    private Map<String, Object> buildEmailParams(FoodOrder order) {
        Map<String, Object> params = new HashMap<>();
        params.put("message", emailMessage(order));
        params.put("order", order);
        params.put("status", order.getStatus());
        params.put("serviceMode", order.getServiceMode().name());
        params.put("customer", order.getCustomer());
        params.put("items", order.getItems());
        params.put("cloudKitchen", order.getCloudKitchen());
        return params;
    }

    private List<String> emailMessage(FoodOrder order) {
       List<String> messages = new ArrayList<>();
        switch (order.getStatus()) {
            case "New" -> {
                messages.add("Your new order");
            }
            case "Paid" -> {
                if (order.getServiceMode() == ServiceMode.COLLECTION) {
                    messages.add("Your order has been Paid. We will notify you once it is Ready for collection");
                } else {
                    messages.add("Your order has been Paid. We will  notify you once it is Ready for delivery");
                }
            }
            case "In Progress" -> {
                messages.add("Your order is in progress");
            }
            case "Ready" -> {
                if (order.getServiceMode() == ServiceMode.COLLECTION) {
                    messages.add("Your order is Ready for collection");
                } else {
                    messages.add("Your order is Ready for Delivery");
                }
            }
            case "Out for delivery" -> {
                messages.add("Your order is Out for Delivery");
            }
            case "Collected" -> {
                messages.add("Your have collected your order.");
            }
            case "Cancelled" -> {
                messages.add("Your order has been cancelled.");
            }
            case "Refund Started" -> {
                messages.add("We have initiated refund on your order.");
            }
            case "Refunded" -> {
                messages.add("Your order has been fully refunded.");
            }
            case "Delivered" -> {
                messages.add("Your order has been delivered.");
            }
            case "Declined" -> {
                messages.add("Sorry the kitchen is unable to pickup your order at this point. ");
                messages.add("Please see the notes from the Kitchen. ");
                messages.add("We are processing your refund and it will be with your account within 5 business days. ");
                messages.add("Please contact the customer support team for further assistance on the refund.");
            }
            case "Payment Error" -> {
                messages.add("Sorry there was an payment error for your order. Please make a payment");
            }
        }
        return messages;
    }

    private void deleteOrder(FoodOrder order) {
        if (order.getStatus().equalsIgnoreCase(OrderStatus.New) || order.getStatus().equalsIgnoreCase(OrderStatus.Cancelled)) {
            customerOrderRepository.delete(order);
            log.info("Order {} has been deleted", order.getReference());
        } else {
            log.info("Order {} cannot be deleted at this point {}", order.getReference(), order.getStatus());
        }
    }

    public void deleteByRef(String ref) {
        final FoodOrder byReference = customerOrderRepository.findByReference(ref);
        if (byReference != null) {
            deleteOrder(byReference);
        }
    }
}
