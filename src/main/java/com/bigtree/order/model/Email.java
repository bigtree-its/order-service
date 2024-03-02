package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class Email {

    String to;
    String subject;
    Map<String, Object> params = new HashMap<>();
}
