package com.bigtree.beku.controller;

import com.bigtree.beku.exception.ApiException;
import com.bigtree.beku.helper.OrderValidator;
import com.bigtree.beku.service.EmailService;
import com.bigtree.beku.model.CustomerOrder;
import com.bigtree.beku.repository.CustomerOrderRepository;
import com.bigtree.beku.service.OrderService;
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

    @Value("${application.send-email-confirmation}")
    private boolean sendEmailConfirmation;

    @Autowired
    CustomerOrderRepository repository;

    @Autowired
    OrderValidator orderValidator;

    @Autowired
    EmailService emailService;

    @Autowired
    OrderService orderService;

    @PostMapping("")
    public ResponseEntity<CustomerOrder> create(@RequestBody CustomerOrder order) {
        log.info("Request create order {}", order);
        orderValidator.validateOrder(order);
        final CustomerOrder saved = orderService.createOrder(order);
        if (saved != null) {
            log.info("Order saved: {}", saved.getReference());
            if ( sendEmailConfirmation){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendOrderConfirmation(saved);
                    }
                }).start();
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }else{
            log.error("Order creation failed");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }

    }

    @GetMapping("")
    public List<CustomerOrder> getAllOrders() {
        log.info("Request get all {}");
        List<CustomerOrder> all = repository.findAll();
        log.info("Returning {} customer orders", all.size());
        return all;
    }

    @DeleteMapping("")
    public ResponseEntity<Void> deleteAllOrders() {
        log.info("Request delete all {}");
        repository.deleteAll();
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOne(@PathVariable("orderId") String id) {
        log.info("Request delete one {}", id);
        repository.deleteById(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<CustomerOrder> getOne(@PathVariable("orderId") String id) {
        log.info("Request get one {}", id);
        final Optional<CustomerOrder> byId = repository.findById(id);
        log.info("Returning customer order with id: {}", byId.get());
        return ResponseEntity.ok(byId.get());
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<CustomerOrder> getOneByReference(@PathVariable("reference") String reference) {
        log.info("Request get one {}", reference);
        final CustomerOrder byReference = repository.findFirstByReference(reference);
        log.info("Returning one customer order with reference: {}", reference);
        return ResponseEntity.ok(byReference);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerOrder>> searchOrders(
            @RequestParam(value = "reference", required = false) String reference ,
            @RequestParam(value = "customer", required = false) String customer,
            @RequestParam(value = "supplier", required = false) String supplier,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDate dateTo
    ) {
        log.info("Request to search orders");
        final List<CustomerOrder> result = orderService.search(reference, customer, supplier, date, dateFrom, dateTo);
        log.info("Returning {} orders for search {}", result.size());
        return ResponseEntity.ok(result);
    }


    @PutMapping("/{orderId}")
    public ResponseEntity<Void> create(@RequestBody CustomerOrder order, @PathVariable("orderId") String orderId) {
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

    private void sendOrderConfirmation(CustomerOrder order) {
        String subject = "Your #FirstBites order " + order.getReference();
        Map<String, Object> body = new HashMap<>();
        body.put("order", order);
        body.put("customer", order.getCustomer());
        body.put("items", order.getItems());
        body.put("supplier", order.getSupplier());
        emailService.sendMail(order.getCustomer().getEmail(), subject, "order", body);
    }
}
