package com.example.customerservice.data;

import lombok.Data;

import java.util.Objects;
import java.util.Set;

@Data
public class UserDto {
    private String name;
    private Set<ERole> roles;

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof UserDto user))
            return false;

        return name.equals(user.name);
    }
}
