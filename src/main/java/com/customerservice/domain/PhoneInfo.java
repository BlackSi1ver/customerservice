package com.customerservice.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;

@Embeddable
@EqualsAndHashCode
public class PhoneInfo {
    private String phoneNumber;
    private Integer country;
    private Integer city;

    protected PhoneInfo() {}

    public PhoneInfo(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Integer getCountry() {
        return country;
    }

    public Integer getCity() {
        return city;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.city = null;
        this.country = null;
    }

    public void setCountry(final Integer country) {
        this.country = country;
    }

    public void setCity(final Integer city) {
        this.city = city;
    }
}
