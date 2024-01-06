package com.bigtree.order;

import com.bigtree.order.model.CustomerOrder;
import com.bigtree.order.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
public class OrderServiceTest {

    @Autowired
    OrderService orderService;

    @Test
    public void testCreteOrder() {
        final CustomerOrder dummyCustomerOrder = DummyData.createDummyOrder();
        final CustomerOrder order = orderService.createOrder(dummyCustomerOrder);
        Assertions.assertNotNull(order);
    }

    @Test
    public void testSearchOrders() {
        final CustomerOrder dummyCustomerOrder = DummyData.createDummyOrder();
        final CustomerOrder order = orderService.createOrder(dummyCustomerOrder);
        Assertions.assertNotNull(order);

        List<CustomerOrder> orders = orderService
                .search(dummyCustomerOrder.getReference(),
                        null,
                        null,
                        null,
                        null,
                        null);
        Assertions.assertNotNull(orders);
        Assertions.assertEquals(1, orders.size());

        // By Customer Email
       orders = orderService
                .search(null,
                        order.getCustomer().getEmail(),
                        null,
                        null,
                        null,
                        null);
        Assertions.assertNotNull(orders);

        // By Supplier Email
        orders = orderService
                .search(null,
                        null,
                        "email",
                        null,
                        null,
                        null);
        Assertions.assertNotNull(orders);

        // By Date
        orders = orderService
                .search(null,
                        null,
                        "email",
                        LocalDate.now(),
                        null,
                        null);
        Assertions.assertNotNull(orders);

    }
}
