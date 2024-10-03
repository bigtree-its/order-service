package com.bigtree.order;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.model.*;
import com.bigtree.order.service.ProfileService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
public class ProfileServiceTest {

    @Autowired
    ProfileService profileService;


    @Test
    public void buildSalesProfile(){
        SalesProfile profile = profileService.getSalesProfile("669ea4a931126cc701dd5f99");
        System.out.println(profile);
    }

    @Test
    public void buildProfile(){
        List<DummyOrder> orders = DummyData.createOrders();
        Map<YearMonth, List<DummyOrder>> ordersByMonth = orders.stream()
                .collect(Collectors.groupingBy(m -> YearMonth.from(m.getDate()), Collectors.toList()));
        ordersByMonth.entrySet().forEach(System.out::println);
    }

    @Test
    public void testThrowsExceptionWhenProfileTypeNotSupplied() {
        ProfileRequest request = ProfileRequest.builder().build();
        final ApiException apiException = Assertions.assertThrows(ApiException.class, () -> {
            final ProfileResponse response = profileService.getProfile(null, null, null, null, null);
        });

        Assertions.assertEquals(apiException.getMessage(), "Profile type is mandatory");
    }

    @Test
    public void testThrowsExceptionWhenProfileIDNotSupplied() {
        ProfileRequest request = ProfileRequest.builder().profileType("Supplier").build();
        final ApiException apiException = Assertions.assertThrows(ApiException.class, () -> {
            final ProfileResponse response = profileService.getProfile(null, null, null, null, null);
        });

        Assertions.assertEquals(apiException.getMessage(), "Either profile email or ID is mandatory");
    }

    @Test
    public void testProfile(){
        final ProfileResponse response = profileService.getProfile("nava.arul@gmail.com", null,null,null,null);
        Assertions.assertNotNull(response);
    }


}
