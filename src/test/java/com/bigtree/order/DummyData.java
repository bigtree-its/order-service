package com.bigtree.order;

import com.bigtree.order.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DummyData {

    public static FoodOrder createDummyOrder() {
        Item i = createDummyItem();
        List<Item> items = new ArrayList<>();
        items.add(i);
        FoodOrder order = FoodOrder.builder()
                .status(OrderStatus.Created)
                .serviceFee(BigDecimal.ONE)
                .subTotal(BigDecimal.ONE)
                .total(BigDecimal.ONE)
                .deliveryFee(BigDecimal.ONE)
                .packingFee(BigDecimal.ONE)
                .serviceMode(ServiceMode.DELIVERY)
                .customer(createDummyCustomer())
                .supplier(createDummySupplier())
                .items(items)
                .createdAt(LocalDateTime.now())
                .expectedDeliveryDate(LocalDate.now())
                .collectionDate(LocalDate.now())
                .build();
        return order;
    }

    public static FoodSupplier createDummySupplier() {
        return FoodSupplier.builder()
                .name("name")
                .mobile("mobile")
                .email("email")
                .address(Address.builder()
                        .addressLine1("addressLine1")
                        .addressLine2("addressLine2")
                        .city("city")
                        .postcode("postcode")
                        .country("GB")
                        .build())
                .build();
    }

    public static Customer createDummyCustomer() {
        return Customer.builder()
                .name("name")
                .mobile("mobile")
                .email("email")
                .address(Address.builder()
                        .addressLine1("addressLine1")
                        .addressLine2("addressLine2")
                        .city("city")
                        .postcode("postcode")
                        .country("GB")
                        .build())
                .build();
    }

    public static  Item createDummyItem() {
        Item i = Item.builder()
                .name("name")
                .id("sd")
                .image("akjdhaskdkjad")
                .price(BigDecimal.ONE)
                .subTotal(BigDecimal.ONE)
                .quantity(BigDecimal.ONE)
                ._tempId("9870808")
                .build();
        return i;
    }
}
