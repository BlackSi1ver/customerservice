package com.customerservice.rest.domain;

import com.customerservice.domain.ERole;
import lombok.Data;

import java.util.Set;

@Data
public class UserDto {

    private long id;
    private String name;
    private Set<ERole> roles;
}
