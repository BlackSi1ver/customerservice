package com.example.customerservice.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClaimRequest {

    long id;

    private String phone;

    private String request;
}
