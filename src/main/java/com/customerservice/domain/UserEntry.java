package com.customerservice.domain;

import jakarta.persistence.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "users")
public class UserEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "name", unique=true, nullable = false)
    private String name;

    @Column(name = "roles", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<ERole> roles;

    @OneToOne(fetch = LAZY, cascade = CascadeType.ALL, mappedBy = "user")
    private UserPasswordEntry userPassword;

    @OneToMany(fetch = LAZY, cascade = CascadeType.ALL, mappedBy = "user")
    private List<ClaimEntry> claims;

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

    protected UserEntry() {}

    public UserEntry(final String name,
                     final Set<ERole> roles) {
        this.name = name;
        this.roles = roles;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<ERole> getRoles() {
        return roles;
    }

}
