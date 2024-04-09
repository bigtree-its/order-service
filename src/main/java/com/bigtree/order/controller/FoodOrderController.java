package com.bigtree.order.controller;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.helper.FoodOrderValidator;
import com.bigtree.order.model.FoodOrder;
import com.bigtree.order.model.OrderUpdateRequest;
import com.bigtree.order.service.EmailService;
import com.bigtree.order.repository.FoodOrderRepository;
import com.bigtree.order.service.FoodOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/orders/v1/foods")
@CrossOrigin(origins = "*")
public class FoodOrderController {

    @Autowired
    FoodOrderRepository repository;

    @Autowired
    FoodOrderValidator foodOrderValidator;

    @Autowired
    EmailService emailService;

    @Autowired
    FoodOrderService foodOrderService;

    @PostMapping("")
    public ResponseEntity<FoodOrder> create(@RequestBody FoodOrder order, @RequestParam(value = "action", required = false) String action) {
        log.info("Request to create food order {}", order);
        foodOrderValidator.validateOrder(order);
        final FoodOrder saved = foodOrderService.createOrder(order, action);
        if (saved != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } else {
            log.error("Order creation failed");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @GetMapping("")
    public ResponseEntity<List<FoodOrder>> getAllOrders(
            @RequestParam(value = "intent", required = false) String intentId,
            @RequestParam(value = "ref", required = false) String reference,
            @RequestParam(value = "customer", required = false) String customer,
            @RequestParam(value = "supplier", required = false) String supplier,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDate dateTo) {
        log.info("Request to search orders");
        final List<FoodOrder> result = foodOrderService.search(intentId, reference, customer, supplier, date, dateFrom, dateTo);
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
        foodOrderService.deleteByRef(ref);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<FoodOrder> getOne(@PathVariable("orderId") String id) {
        log.info("Request get one {}", id);
        final Optional<FoodOrder> byId = repository.findById(id);
        log.info("Returning customer order with id: {}", byId.get());
        return ResponseEntity.ok(byId.get());
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<FoodOrder> updateOrder(@RequestBody FoodOrder order, @PathVariable("orderId") String orderId) {
        log.info("Request update order {}", orderId);
        final Optional<FoodOrder> byId = repository.findById(orderId);
        if (byId.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order not found", "Order not found with id " + orderId);
        }
        final FoodOrder foodOrder = byId.get();
        foodOrder.setUpdatedAt(LocalDateTime.now());
        foodOrder.setStatus(order.getStatus());
        return ResponseEntity.status(HttpStatusCode.valueOf(202)).build();
    }

    @PutMapping("")
    public ResponseEntity<FoodOrder> updateOrder(@RequestBody OrderUpdateRequest orderUpdateRequest) {
        log.info("Request to update order  {}", orderUpdateRequest);
        FoodOrder foodOrder = foodOrderService.update(orderUpdateRequest);
        return ResponseEntity.ok(foodOrder);
    }

    @PutMapping("/action")
    public ResponseEntity<FoodOrder> action(@RequestParam String ref, @RequestParam String action) {
        log.info("Request to {} order  {}", action, ref);
        FoodOrder foodOrder = foodOrderService.action(ref, action);
        return ResponseEntity.ok(foodOrder);
    }

}
