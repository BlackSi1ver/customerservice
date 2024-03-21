package com.customerservice.domain;

import jakarta.persistence.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "passwords")
public class UserPasswordEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @OneToOne(fetch = LAZY)
    private UserEntry user;

    @Column(name = "password", nullable = false)
    private String password;

    protected UserPasswordEntry() {}

    public UserPasswordEntry(final UserEntry user,
                             final String password) {
        this.user = user;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
