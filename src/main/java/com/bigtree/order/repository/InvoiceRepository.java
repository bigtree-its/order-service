package com.bigtree.order.repository;

import com.bigtree.order.model.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Invoice findByOrderReference(String reference);

    List<Invoice> findAllByCloudKitchenId(String cloudKitchenId);
}
