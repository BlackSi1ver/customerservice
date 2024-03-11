package com.example.customerservice.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserEntry {
    @Id
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "roles")
    @Enumerated(EnumType.STRING)
    private Set<ERole> roles;

    @Column(name = "password", nullable = false)
    private String password;

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof UserEntry user))
            return false;

        return name.equals(user.name);
    }
}
