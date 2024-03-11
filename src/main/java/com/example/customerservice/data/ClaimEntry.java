package com.example.customerservice.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "claims")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClaimEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "username", referencedColumnName = "name", nullable = false)
    private UserEntry user;

    @Column(name = "status", nullable = false, length = 8)
    @Enumerated(EnumType.STRING)
    private ERequestStatus status;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "country")
    private Integer country;

    @Column(name = "city")
    private Integer city;

    @Column(name = "request")
    private String request;

    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Override
    public int hashCode() {
        return Objects.hash(id, user);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof ClaimEntry claim))
            return false;

        return id == claim.id && user.equals(claim.user);
    }
}
