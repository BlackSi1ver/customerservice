package com.customerservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "phone", url="https://cleaner.dadata.ru/api/v1/clean/phone")
public interface ExternalPhoneInfoApi {

    @PostMapping
    String getPhoneDetails(@RequestHeader Map headers, @RequestBody String body);
}
