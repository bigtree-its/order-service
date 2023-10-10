package com.bigtree.beku.helper;

import com.bigtree.beku.exception.ApiException;
import com.bigtree.beku.model.Customer;
import com.bigtree.beku.model.Item;
import com.bigtree.beku.model.Supplier;
import com.bigtree.beku.model.CustomerOrder;
import com.bigtree.beku.model.ServiceMode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
@Slf4j
public class OrderValidator {

    public void validateOrder(CustomerOrder order){
        if (CollectionUtils.isEmpty(order.getItems())){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Items","Items cannot be empty");
        }
        validateItems(order.getItems());
        validateSupplier(order.getSupplier());
        validateCustomer(order.getCustomer(), order.getServiceMode() == ServiceMode.DELIVERY ? true: false);
    }

    private void validateSupplier(Supplier supplier){
        if ( supplier == null ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Supplier", "Supplier cannot be empty");
        }
        if ( supplier.get_id() == null ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Supplier", "SupplierId cannot be empty");
        }
        if (StringUtils.isEmpty(supplier.getEmail()) ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Supplier", "Supplier email cannot be empty");
        }
        if (StringUtils.isEmpty(supplier.getMobile()) ){
            throw new ApiException(HttpStatus.BAD_REQUEST,"Supplier", "Supplier Mobile cannot be empty");
        }
        if (supplier.getAddress() == null
                || StringUtils.isEmpty(supplier.getAddress().getAddressLine1())
                || StringUtils.isEmpty(supplier.getAddress().getAddressLine2())
                || StringUtils.isEmpty(supplier.getAddress().getCity())
                || StringUtils.isEmpty(supplier.getAddress().getCountry())
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
                  || StringUtils.isEmpty(customer.getAddress().getCountry())
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
