package com.bigtree.order.service;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.model.*;
import com.bigtree.order.repository.FoodOrderRepository;
import com.bigtree.order.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class InvoiceService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Autowired
    FoodOrderRepository orderRepository;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    public Invoice handleInvoiceRequest(InvoiceRequest request){
        if (StringUtils.isEmpty(request.getOrderReference())){
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient Data", "");
        }
        if (StringUtils.isEmpty(request.getCloudKitchenId())){
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient Data", "");
        }
        if (StringUtils.isEmpty(request.getAction())){
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient Data", "");
        }
        log.info("Handling Invoice Request for CloudKitchen {}", request.getCloudKitchenId());
        return switch (request.getAction()) {
            case "Create" -> createInvoice(request);
            case "Accept" -> accept(request);
            case "Complete" -> complete(request);
            default -> null;
        };
    }

    private Invoice createInvoice(InvoiceRequest request) {
        Invoice invoice = invoiceRepository.findByOrderReference(request.getOrderReference());
        if (invoice != null) {
            log.error("An invoice already exist for order {}", request.getOrderReference());
        } else {
            FoodOrder foodOrder = orderRepository.findByReference(request.getOrderReference());
            if (foodOrder != null) {
                if (foodOrder.getStatus().equalsIgnoreCase(OrderStatus.Collected) || foodOrder.getStatus().equalsIgnoreCase(OrderStatus.Delivered)) {
                    log.info("Creating a invoice for order {}", request.getOrderReference());
                    BigDecimal commission = BigDecimal.valueOf(7).multiply(foodOrder.getTotal()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
                    BigDecimal invoiceAmount = foodOrder.getTotal().subtract(commission);
                    Invoice newInvoice = Invoice.builder()
                            .cloudKitchenId(request.getCloudKitchenId())
                            .orderReference(request.getOrderReference())
                            .orderAmount(foodOrder.getTotal())
                            .status(InvoiceStatus.Pending)
                            .invoiceAmount(invoiceAmount)
                            .date(LocalDate.now())
                            .build();

                    Invoice created = invoiceRepository.save(newInvoice);
                    log.info("Invoice {} created for order {} with amount {}", created.get_id(), request.getOrderReference(), newInvoice.getInvoiceAmount());
                    foodOrder.setStatus(OrderStatus.Invoiced);
                    foodOrder.setDateInvoiced(LocalDateTime.now());
                    orderRepository.save(foodOrder);
                    return created;
                }else{
                    log.error("The order {} cannot be invoiced at the moment. Its {}", request.getOrderReference(), foodOrder.getStatus());
                }
            }else{
                log.error("NO order found for reference {}", request.getOrderReference());
            }
        }
        return invoice;
    }

    private Invoice accept(InvoiceRequest request) {
        log.info("Accept Invoice Request for CloudKitchen {}", request.getCloudKitchenId());
        FoodOrder foodOrder = orderRepository.findByReference(request.getOrderReference());
        Invoice invoice = invoiceRepository.findByOrderReference(request.getOrderReference());
        if( invoice == null){
            log.error("Accept Invoice request failed. Invoice not found for order {}", request.getOrderReference());
        } else if (!invoice.getStatus().equalsIgnoreCase(InvoiceStatus.Pending)){
            log.error("Accept Invoice request failed for order {}. Invoice not in correct state {}", request.getOrderReference(), invoice.getStatus());
        }else if( foodOrder == null){
            log.error("Accept Invoice request failed. Order not found {}", request.getOrderReference());
        } else{
            invoice.setStatus(InvoiceStatus.Accepted);
            invoice.setDateAccepted(LocalDate.now());
            invoice.setAcceptedAt(LocalDateTime.now());
            invoiceRepository.save(invoice);
            log.info("Accepted an invoice for order {}", request.getOrderReference());
            foodOrder.setStatus(OrderStatus.Invoice_Accepted);
            foodOrder.setDateInvoiceAccepted(LocalDateTime.now());
            orderRepository.save(foodOrder);
            return invoice;
        }
        return null;
    }

    private Invoice complete(InvoiceRequest request) {
        log.info("Complete Invoice Request for CloudKitchen {}", request.getCloudKitchenId());
        FoodOrder foodOrder = orderRepository.findByReference(request.getOrderReference());
        Invoice invoice = invoiceRepository.findByOrderReference(request.getOrderReference());
        if( invoice == null){
            log.error("Complete Invoice request failed. Invoice not found for order {}", request.getOrderReference());
        } else if (!invoice.getStatus().equalsIgnoreCase(InvoiceStatus.Accepted)){
            log.error("Complete Invoice request failed for order {}. Invoice not in correct state {}", request.getOrderReference(), invoice.getStatus());
        }else if(foodOrder == null){
            log.error("Complete Invoice request failed. Order not found {}", request.getOrderReference());
        } else{
            invoice.setStatus(InvoiceStatus.Completed);
            invoice.setDateCompleted(LocalDate.now());
            invoice.setCompletedAt(LocalDateTime.now());
            invoiceRepository.save(invoice);
            log.info("Completed an invoice for order {}", request.getOrderReference());
            foodOrder.setStatus(OrderStatus.Completed);
            foodOrder.setDateCompleted(LocalDateTime.now());
            orderRepository.save(foodOrder);
            return invoice;
        }
        return null;
    }

    public List<Invoice> getInvoices(String orderReference, String cloudKitchenId, LocalDate date, LocalDate dateFrom, LocalDate dateTo) {
        List<Invoice> result = new ArrayList<>();
        Query query = new Query();
        if (StringUtils.isNotEmpty(orderReference)) {
            log.info("Searching invoice for order reference {}", orderReference);
            Invoice invoice = invoiceRepository.findByOrderReference(orderReference);
            if (invoice != null) {
                result.add(invoice);
                return result;
            }
        }

        if (StringUtils.isNotEmpty(cloudKitchenId)) {
            query.addCriteria(Criteria.where("cloudKitchenId").is(cloudKitchenId));
        }
        if (date != null) {
            query.addCriteria(Criteria.where("date").is(date));
        } else {
            if (dateFrom != null && dateTo != null) {
                query.addCriteria(Criteria.where("date").gte(dateFrom).lte(dateTo));
            } else if (dateTo != null) {
                query.addCriteria(Criteria.where("date").lte(dateTo));
            } else if (dateFrom != null) {
                query.addCriteria(Criteria.where("date").gte(dateFrom));
            }
        }

        log.info("Searching invoices with query {}", query.toString());
        result = mongoTemplate.find(query, Invoice.class);
        return result;
    }

    public Invoice getOne(String id) {
        Optional<Invoice> r = invoiceRepository.findById(id);
        return r.orElse(null);
    }

    public void deleteOne(String id) {
        Optional<Invoice> ops = invoiceRepository.findById(id);
        if (ops.isPresent()){
            FoodOrder foodOrder = orderRepository.findByReference(ops.get().getOrderReference());
            if ( foodOrder != null){
                foodOrder.setDateInvoiced(null);
                foodOrder.setDateInvoiceAccepted(null);
                foodOrder.setDateCompleted(null);
                if(foodOrder.getServiceMode() == ServiceMode.COLLECTION){
                    foodOrder.setStatus(OrderStatus.Collected);
                }
                if(foodOrder.getServiceMode() == ServiceMode.DELIVERY){
                    foodOrder.setStatus(OrderStatus.Delivered);
                }
                orderRepository.save(foodOrder);
            }
        }
        invoiceRepository.deleteById(id);
    }
}
