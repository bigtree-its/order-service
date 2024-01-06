package com.bigtree.order;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.model.ProfileRequest;
import com.bigtree.order.model.ProfileResponse;
import com.bigtree.order.service.ProfileService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ProfileServiceTest {

    @Autowired
    ProfileService profileService;

    @Test
    public void testThrowsExceptionWhenProfileTypeNotSupplied() {
        ProfileRequest request = ProfileRequest.builder().build();
        final ApiException apiException = Assertions.assertThrows(ApiException.class, () -> {
            final ProfileResponse response = profileService.getProfile(request);
        });

        Assertions.assertEquals(apiException.getMessage(), "Profile type is mandatory");
    }

    @Test
    public void testThrowsExceptionWhenProfileIDNotSupplied() {
        ProfileRequest request = ProfileRequest.builder().profileType("Supplier").build();
        final ApiException apiException = Assertions.assertThrows(ApiException.class, () -> {
            final ProfileResponse response = profileService.getProfile(request);
        });

        Assertions.assertEquals(apiException.getMessage(), "Either profile email or ID is mandatory");
    }

    @Test
    public void testProfile(){
        ProfileRequest request = ProfileRequest.builder().profileType("Supplier").profileEmail("nava.arul@gmail.com").build();
        final ProfileResponse response = profileService.getProfile(request);
        Assertions.assertNotNull(response);
    }


}
