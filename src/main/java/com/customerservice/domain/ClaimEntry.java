package com.customerservice.domain;

import jakarta.persistence.*;

import java.util.Date;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "claims")
public class ClaimEntry {

    @Version
    private long version;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = LAZY)
    private UserEntry user;

    @Column(name = "status", nullable = false, length = 8)
    @Enumerated(EnumType.STRING)
    private ERequestStatus status;

    @Embedded
    @Column(name = "phone", nullable = false, length = 20)
    private PhoneInfo phone;

    @Column(name = "request")
    private String request;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    protected ClaimEntry() {}

    public ClaimEntry(final UserEntry user,
                      final ERequestStatus status,
                      final PhoneInfo phone,
                      final String request,
                      final Date createdAt) {
        this.user = user;
        this.status = status;
        this.phone = phone;
        this.request = request;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public ERequestStatus getStatus() {
        return status;
    }

    public UserEntry getUser() {
        return user;
    }

    public String getRequest() {
        return request;
    }

    public PhoneInfo getPhone() {
        return phone;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setStatus(final ERequestStatus status) {
        this.status = status;
    }

    public void setPhone(final String phone) {
        this.phone.setPhoneNumber(phone);
    }

    public void setCountry(final Integer country) {
        this.phone.setCountry(country);
    }

    public void setCity(final Integer city) {
        this.phone.setCity(city);
    }

    public void setRequest(final String request) {
        this.request = request;
    }
}
