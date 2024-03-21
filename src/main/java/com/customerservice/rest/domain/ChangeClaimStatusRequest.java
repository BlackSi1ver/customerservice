package com.customerservice.rest.domain;

import com.customerservice.domain.ERequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeClaimStatusRequest {
    private ERequestStatus status;
}
