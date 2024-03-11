package com.example.customerservice.data;

import lombok.Data;

import java.util.Date;

@Data
public class ClaimDto {

    private long id;

    private String username;

    private ERequestStatus status;

    private String phone;

    private Integer country;

    private Integer city;

    private String request;

    private Date createdAt;
}
