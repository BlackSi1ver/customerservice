package com.customerservice.rest.domain;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
