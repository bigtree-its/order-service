package com.bigtree.order;

import com.bigtree.order.model.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DummyData {

    public static List<DummyOrder> createOrders(){
        List<DummyOrder> items = new ArrayList<>();
        LocalDate firstDayOfYear = LocalDate.now()
                .withDayOfMonth(1)
                .withMonth(1)
                .minusYears(1);
        int month = 1;
        for(int i=1; i<25; i++){
            items.add(DummyOrder.builder().ref(RandomStringUtils.random(5)).date(firstDayOfYear).build());
            items.add(DummyOrder.builder().ref(RandomStringUtils.random(5)).date(firstDayOfYear).build());
            if ( month == 12){
                month = 1;
                firstDayOfYear = firstDayOfYear.withMonth(1).withDayOfMonth(1).plusYears(1);
            }else{
                firstDayOfYear = firstDayOfYear.plusMonths(1);
            }
            month++;
        }
        return items;
    }

    public static FoodOrder createDummyOrder() {
        Item i = createDummyItem();
        List<Item> items = new ArrayList<>();
        items.add(i);
        FoodOrder order = FoodOrder.builder()
                .status(OrderStatus.Unpaid)
                .serviceFee(BigDecimal.ONE)
                .subTotal(BigDecimal.ONE)
                .total(BigDecimal.ONE)
                .deliveryFee(BigDecimal.ONE)
                .packingFee(BigDecimal.ONE)
                .serviceMode(ServiceMode.DELIVERY)
                .customer(createDummyCustomer())
                .cloudKitchen(createDummySupplier())
                .items(items)
                .createdAt(LocalDateTime.now())
                .expectedDeliveryDate(LocalDate.now())
                .build();
        return order;
    }

    public static CloudKitchen createDummySupplier() {
        return CloudKitchen.builder()
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
