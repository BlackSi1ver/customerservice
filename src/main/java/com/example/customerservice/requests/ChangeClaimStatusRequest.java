package com.example.customerservice.requests;

import com.example.customerservice.data.ERequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeClaimStatusRequest {
    long id;

    private ERequestStatus status;
}
