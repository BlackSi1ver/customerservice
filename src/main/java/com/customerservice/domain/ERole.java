package com.customerservice.domain;

import org.springframework.security.core.GrantedAuthority;

public enum ERole implements GrantedAuthority {

    ADMIN("ADMIN"),
    OPERATOR("OPERATOR"),
    USER("USER");

    private final String value;

    ERole(final String value) {
        this.value = value;
    }

    @Override
    public String getAuthority() {
        return value;
    }
}
