package com.bigtree.order.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Note {

    String message;
    LocalDateTime dateTime;
}