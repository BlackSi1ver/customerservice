package com.customerservice.rest.domain;

import com.customerservice.domain.ERequestStatus;
import com.customerservice.domain.PhoneInfo;
import lombok.Data;

import java.sql.Timestamp;


@Data
public class ClaimDto {

    private long id;
    private long userId;
    private ERequestStatus status;
    private PhoneInfo phone;
    private String request;
    private Timestamp createdAt;
}
