package com.example.customerservice.servicies;

import com.example.customerservice.client.ExternalPhoneInfoApi;
import com.example.customerservice.data.PhoneDetails;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PhoneDetailsService {

    @Value("${dadata.api.token}")
    private String daDataApiToken;
    @Value("${dadata.api.secret}")
    private String daDataApiSecret;

    @Autowired
    private ExternalPhoneInfoApi externalDaDataApi;

    public PhoneDetails getPhoneDetails(String phone) {
        final PhoneDetails details = new PhoneDetails();

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.put(HttpHeaders.AUTHORIZATION, "Token " + daDataApiToken);
        headers.put("X-Secret", daDataApiSecret);

        try {
            String data = externalDaDataApi.getPhoneDetails(
                    headers,
                    "[ \"" + phone + "\" ]"
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
                    details.setCountry(Integer.valueOf(value));
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }

            value = map.get("city_code");
            if (value != null) {
                try {
                    details.setCity(Integer.valueOf(value));
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }

        } catch (Exception e) {
            // Do nothing
        }

        return details;
    }
}
