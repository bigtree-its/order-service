package com.bigtree.beku.repository;

import com.bigtree.beku.model.LocalPaymentIntent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository class for <code>LocalPaymentIntent</code> domain objects All method names are compliant
 * with Spring Data naming conventions so this interface can easily be extended for Spring
 * Data. See:
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
 *
 * @author Nav
 */
public interface PaymentRepository extends MongoRepository<LocalPaymentIntent, String> {

    LocalPaymentIntent findFirstByOrderReference(String orderId);

    LocalPaymentIntent findFirstByIntentId(String intentId);

    LocalPaymentIntent findFirstByClientSecret(String clientSecret);

    List<LocalPaymentIntent> findByCustomer(String customer);

    List<LocalPaymentIntent> findBySupplier(String supplier);

}
