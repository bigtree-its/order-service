package com.bigtree.order.repository;

import com.bigtree.order.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository class for <code>Payment</code> domain objects All method names are compliant
 * with Spring Data naming conventions so this interface can easily be extended for Spring
 * Data. See:
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
 *
 * @author Nav
 */
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Payment findFirstByOrderReference(String orderReference);

    Payment findFirstByIntentId(String intentId);

    Payment findFirstByClientSecret(String clientSecret);

    List<Payment> findByCustomer(String customer);

    List<Payment> findBySupplier(String supplier);

    List<Payment> findByStatus(String status);
}
