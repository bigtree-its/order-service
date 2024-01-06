package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Map;

@Document(collection = "payments")
@Data
@Builder
public class LocalPaymentIntent {

    @Id
    private String id;
    private String intentId;
    private String object;
    private BigDecimal amount;
    private String customer;
    private String orderReference;
    private String clientSecret;
    private String currency;
    private boolean error;
    private boolean liveMode;
    private String errorMessage;
    private String paymentMethod;
    private String status;
    private String supplier;
    private String chargesUrl;
    private Map<String,String> metaData;
}
