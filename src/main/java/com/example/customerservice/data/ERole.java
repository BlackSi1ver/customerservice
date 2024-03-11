package com.example.customerservice.data;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@RequiredArgsConstructor
public enum ERole implements GrantedAuthority {

    ADMIN("ADMIN"),
    OPERATOR("OPERATOR"),
    USER("USER");

    private final String value;

    @Override
    public String getAuthority() {
        return value;
    }
}
