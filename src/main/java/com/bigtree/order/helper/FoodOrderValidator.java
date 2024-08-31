package com.bigtree.order.helper;

import com.bigtree.order.exception.ApiException;
import com.bigtree.order.model.*;
import com.bigtree.order.model.CloudKitchen;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
@Slf4j
public class FoodOrderValidator {

    public void validateOrder(FoodOrder order){

        if (!order.isPartyOrder() && CollectionUtils.isEmpty(order.getItems())){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Items","Items cannot be empty");
        }
        if (order.isPartyOrder() && CollectionUtils.isEmpty(order.getPartyItems())){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Items","Items cannot be empty");
        }
        validateItems(order.getItems());
        validateSupplier(order.getCloudKitchen());
        validateCustomer(order.getCustomer(), order.getServiceMode() == ServiceMode.DELIVERY);
    }

    private void validateSupplier(CloudKitchen cloudKitchen){
        if ( cloudKitchen == null ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Supplier", "Supplier cannot be empty");
        }
        if ( cloudKitchen.get_id() == null ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Supplier", "SupplierId cannot be empty");
        }
        if (StringUtils.isEmpty(cloudKitchen.getEmail()) ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Supplier", "Supplier email cannot be empty");
        }
        if (StringUtils.isEmpty(cloudKitchen.getMobile()) ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Supplier", "Supplier Mobile cannot be empty");
        }
        if (cloudKitchen.getAddress() == null
                || StringUtils.isEmpty(cloudKitchen.getAddress().getAddressLine1())
                || StringUtils.isEmpty(cloudKitchen.getAddress().getAddressLine2())
                || StringUtils.isEmpty(cloudKitchen.getAddress().getCity())
                || StringUtils.isEmpty(cloudKitchen.getAddress().getCountry())
        ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Supplier", "Supplier Address cannot be empty");
        }
    }

    private void validateCustomer(Customer customer, boolean validateAddress){
        if ( customer == null ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Customer", "Customer cannot be empty");
        }
        if (StringUtils.isEmpty(customer.getName()) ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Customer", "Customer name cannot be empty");
        }
        if (StringUtils.isEmpty(customer.getEmail()) ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Customer", "Customer email cannot be empty");
        }
        if (StringUtils.isEmpty(customer.getMobile()) ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Customer", "Customer Mobile cannot be empty");
        }
        if ( validateAddress){
          if (customer.getAddress() == null
                  || StringUtils.isEmpty(customer.getAddress().getAddressLine1())
                  || StringUtils.isEmpty(customer.getAddress().getAddressLine2())
                  || StringUtils.isEmpty(customer.getAddress().getCity())
                  || StringUtils.isEmpty(customer.getAddress().getPostcode())
          ){
              throw new ApiException(HttpStatus.BAD_REQUEST,"Customer", "Customer Address cannot be empty");
          }
        }

    }

    private void validateItems(List<Item> items){
        for (Item item : items) {
            if ( item.getId() == null ){
                throw new ApiException(HttpStatus.BAD_REQUEST,"Item", "Item Id cannot be empty");
            }
            if (StringUtils.isEmpty(item.getName()) ){
                throw new ApiException(HttpStatus.BAD_REQUEST,"Item", "Item name cannot be empty");
            }
            if (item.getQuantity() == null || item.getQuantity().intValue() == 0 ){
                throw new ApiException(HttpStatus.BAD_REQUEST,"Item", "Qty cannot be zero");
            }
        }
    }
}
