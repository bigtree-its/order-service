package com.bigtree.order.repository;

import com.bigtree.order.model.FoodOrder;
import com.bigtree.order.model.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository class for <code>Order</code> domain objects All method names are compliant
 * with Spring Data naming conventions so this interface can easily be extended for Spring
 * Data. See:
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
 *
 * @author Nav
 */
public interface FoodOrderRepository extends MongoRepository<FoodOrder, String> {

    FoodOrder findByReference(String reference);

    /**
     * Retrieve count of <code>Order</code> from the data store for given date
     *
     * @return a count
     */
    @Transactional(readOnly = true)
    Long countByDateCreated(LocalDateTime dateCreated);

    @Transactional(readOnly = true)
    @Query("SELECT o FROM FoodOrder o WHERE o.supplier.id = ?1")
    List<FoodOrder> findBySupplierId(UUID supplierId);

    /**
     * Retrieve all <code>Order</code>s from the data store for given customer's email
     *
     * @return a <code>Collection</code> of <code>Order</code>s
     */
    @Transactional(readOnly = true)
    @Query("SELECT o FROM FoodOrder o WHERE o.customer.email = ?1")
    List<FoodOrder> findByCustomerEmail(String customerEmail);

    @Transactional(readOnly = true)
    @Query("SELECT o FROM FoodOrder o WHERE o.customer.mobile = ?1")
    List<FoodOrder> findByCustomerMobile(String customerMobile);

    /**
     * Retrieve all <code>Order</code>s from the data store for given status
     *
     * @return a <code>Collection</code> of <code>Order</code>s
     */
    @Transactional(readOnly = true)
    @Query("SELECT o FROM FoodOrder o WHERE o.status = ?1")
    Collection<FoodOrder> findByStatus(OrderStatus status);



}
