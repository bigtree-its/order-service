package com.bigtree.order.controller;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.helper.OrderValidator;
import com.bigtree.order.model.OrderUpdateRequest;
import com.bigtree.order.service.EmailService;
import com.bigtree.order.model.CustomerOrder;
import com.bigtree.order.repository.CustomerOrderRepository;
import com.bigtree.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/api/customer-orders")
@CrossOrigin(origins = "*")
public class CustomerOrderController {

    @Autowired
    CustomerOrderRepository repository;

    @Autowired
    OrderValidator orderValidator;

    @Autowired
    EmailService emailService;

    @Autowired
    OrderService orderService;

    @PostMapping("")
    public ResponseEntity<CustomerOrder> create(@RequestBody CustomerOrder order, @RequestParam(value = "action", required = false) String action) {
        log.info("Request create order {}", order);
        orderValidator.validateOrder(order);
        final CustomerOrder saved = orderService.createOrder(order, action);
        if (saved != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } else {
            log.error("Order creation failed");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @GetMapping("")
    public ResponseEntity<List<CustomerOrder>> getAllOrders(
            @RequestParam(value = "intent", required = false) String intentId,
            @RequestParam(value = "ref", required = false) String reference,
            @RequestParam(value = "customer", required = false) String customer,
            @RequestParam(value = "supplier", required = false) String supplier,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDate dateTo) {
        log.info("Request to search orders");
        final List<CustomerOrder> result = orderService.search(intentId, reference, customer, supplier, date, dateFrom, dateTo);
        log.info("Returning {} orders for search", result.size());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("")
    public ResponseEntity<Void> deleteAllOrders() {
        log.info("Request to delete all orders");
        repository.deleteAll();
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{ref}")
    public ResponseEntity<Void> deleteOne(@PathVariable("ref") String ref) {
        log.info("Request delete order {}", ref);
        orderService.deleteByRef(ref);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<CustomerOrder> getOne(@PathVariable("orderId") String id) {
        log.info("Request get one {}", id);
        final Optional<CustomerOrder> byId = repository.findById(id);
        log.info("Returning customer order with id: {}", byId.get());
        return ResponseEntity.ok(byId.get());
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<CustomerOrder> updateOrder(@RequestBody CustomerOrder order, @PathVariable("orderId") String orderId) {
        log.info("Request update order {}", orderId);
        final Optional<CustomerOrder> byId = repository.findById(orderId);
        if (byId.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order not found", "Order not found with id " + orderId);
        }
        final CustomerOrder customerOrder = byId.get();
        customerOrder.setUpdatedAt(LocalDateTime.now());
        customerOrder.setStatus(order.getStatus());
        return ResponseEntity.status(HttpStatusCode.valueOf(202)).build();
    }

    @PutMapping("")
    public ResponseEntity<CustomerOrder> updateOrder(@RequestBody OrderUpdateRequest orderUpdateRequest) {
        log.info("Request to update order  {}", orderUpdateRequest);
        CustomerOrder customerOrder = orderService.update(orderUpdateRequest);
        return ResponseEntity.ok(customerOrder);
    }

    @PutMapping("/action")
    public ResponseEntity<CustomerOrder> action(@RequestParam String ref, @RequestParam String action) {
        log.info("Request to {} order  {}", action, ref);
        CustomerOrder customerOrder = orderService.action(ref, action);
        return ResponseEntity.ok(customerOrder);
    }

}
