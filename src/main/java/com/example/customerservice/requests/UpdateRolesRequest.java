package com.example.customerservice.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolesRequest {

    String username;

    @JsonProperty("set_operator_role")
    boolean setOperatorRole;
}
