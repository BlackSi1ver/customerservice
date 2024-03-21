package com.customerservice.rest.domain;

import lombok.Data;


@Data
public class AuthResponse {

    private String jwt;

    protected AuthResponse() {}

    public AuthResponse(final String jwt) {
        this.jwt = jwt;
    }
}


