package com.bigtree.order.controller;

import com.bigtree.order.model.FoodOrder;
import com.bigtree.order.model.Invoice;
import com.bigtree.order.model.InvoiceRequest;
import com.bigtree.order.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@RestController
@Slf4j
@RequestMapping("/invoices")
@CrossOrigin(exposedHeaders = {"Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"}, allowedHeaders = {"Authorization", "Origin"}, origins = {"http://localhost:4200", "*"})
public class InvoiceController {

    @Autowired
    InvoiceService invoiceService;

    @PostMapping("")
    public ResponseEntity<Invoice> create(@RequestBody InvoiceRequest request) {
        log.info("Received request to create invoice");
        Invoice response = invoiceService.handleInvoiceRequest(request);
        if (response != null) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            log.error("Invoice Request failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("")
    public ResponseEntity<List<Invoice>> getAll(
            @RequestParam(value = "orderReference", required = false) String orderReference,
            @RequestParam(value = "cloudKitchenId", required = false) String cloudKitchenId,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDate dateTo) {
        log.info("Received request to get invoices");
        List<Invoice> response = invoiceService.getInvoices(orderReference, cloudKitchenId, date, dateFrom, dateTo);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getOne(@PathVariable("id") String id) {
        log.info("Request to get one invoice {}", id);
        Invoice invoice = invoiceService.getOne(id);
        log.info("Returning invoice with id: {}", id);
        return ResponseEntity.ok(invoice);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOne(@PathVariable("id") String id) {
        log.info("Request to delete one invoice {}", id);
        invoiceService.deleteOne(id);
        log.info("Deleted an invoice {}", id);
        return ResponseEntity.accepted().build();
    }
}
