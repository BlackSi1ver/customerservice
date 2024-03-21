package com.customerservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class PhoneDetailsService {

    private static final int THREAD_WAIT_TIMEOUT_MS = 1_000;

    @Value("${dadata.api.token}")
    private String daDataApiToken;
    @Value("${dadata.api.secret}")
    private String daDataApiSecret;

    private final ExternalPhoneInfoApi externalDaDataApi;
    private ExternalPhoneDetailsHandler externalPhoneDetailsHandler;
    private final BlockingQueue<PhoneRequest> requestsQueue = new LinkedBlockingQueue<>();
    private final Thread handlingThread = new Thread(new RequestsHandleRunnable());

    public PhoneDetailsService(final ExternalPhoneInfoApi externalDaDataApi) {
        this.externalDaDataApi = externalDaDataApi;
        handlingThread.start();
    }

    public void setExternalDataHandler(final ExternalPhoneDetailsHandler externalPhoneDetailsHandler) {
        this.externalPhoneDetailsHandler = externalPhoneDetailsHandler;
    }

    @AllArgsConstructor
    @Getter
    public static class PhoneRequest {
        final private long claimId;
        final private String phoneNumber;
    }

    public void AddPhoneRequest(final PhoneRequest request) {
        requestsQueue.add(request);
    }

    private class RequestsHandleRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    final PhoneRequest request = requestsQueue.poll(THREAD_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (request != null) {
                        handlePhoneDetailsRequest(request);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private void handlePhoneDetailsRequest(final PhoneRequest request) {

        Integer country = null;
        Integer city = null;

        final Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.put(HttpHeaders.AUTHORIZATION, "Token " + daDataApiToken);
        headers.put("X-Secret", daDataApiSecret);

        try {
            String data = externalDaDataApi.getPhoneDetails(
                    headers,
                    "[ \"" + request.getPhoneNumber() + "\" ]"
            );

            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<HashMap<String,String>> typeRef = new TypeReference<>() {};

            final HashMap<String, String> map = objectMapper.readValue(
                    data.replace('[', ' ').replace(']', ' '),
                    typeRef
            );

            String value = map.get("country_code");
            if (value != null) {
                try {
                    country = Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }

            value = map.get("city_code");
            if (value != null) {
                try {
                    city = Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }

            if (externalPhoneDetailsHandler != null) {
                externalPhoneDetailsHandler.saveData(request.getClaimId(), country, city);
            }

        } catch (Exception e) {
            // Do nothing
            System.out.println("ff");
        }
    }
}
