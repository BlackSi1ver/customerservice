package com.customerservice.rest.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateClaimRequest {
    private String phone;
    private String request;
}
