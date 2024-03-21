package com.customerservice.rest.domain;

import com.customerservice.domain.ERole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRoleRequest {
    ERole role;
    boolean enable;
}
