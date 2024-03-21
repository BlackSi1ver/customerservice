package com.customerservice.service;

@FunctionalInterface
public interface ExternalPhoneDetailsHandler {
    void saveData(long id, Integer country, Integer city);
}
