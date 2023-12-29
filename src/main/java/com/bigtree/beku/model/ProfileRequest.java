package com.bigtree.beku.model;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileRequest {

    private String profileType;
    private String profileId;
    private String profileEmail;
    private String dateFrom;
    private String dateTo;
}
