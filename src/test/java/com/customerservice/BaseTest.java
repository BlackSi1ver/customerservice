package com.customerservice;

import com.customerservice.rest.domain.*;
import com.customerservice.service.ClaimService;
import com.customerservice.service.JwtService;
import com.customerservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BaseTest {

    static final String USER_NAME = "user1";
    static final String USER_PASSWORD = "userpass";
    static final String USER_PHONE = "+79147904444";
    static final String OPERATOR_NAME = "oper1";
    static final String OPERATOR_PASSWORD = "operpass";
    static final String ADMIN_NAME = "admin1";
    static final String ADMIN_PASSWORD = "adminpass";
    static final String USER2_NAME = "user2";

    static final int PAGE_SIZE = 5;

    final int MAX_TEST_CLAIMS_COUNT = 100;

    @LocalServerPort
    private int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ClaimService claimService;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected UserService userService;

    protected String getRootUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    protected String authUserGetJwt(String username, String password) {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername(username);
        authRequest.setPassword(password);

        ResponseEntity<AuthResponse> postResponse = restTemplate.postForEntity(
                getRootUrl() + "/auth/login", authRequest, AuthResponse.class
        );

        assertNotNull(postResponse);
        assertEquals(postResponse.getStatusCode(), HttpStatus.OK);
        assertNotNull(postResponse.getBody());

        final String jwt = postResponse.getBody().getJwt();
        System.out.println(jwt);
        return jwt;
    }

    protected HttpHeaders getHeaderWithJwt(String jwt) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    protected void logoutByJwt(String jwt) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<UpdateUserRoleRequest> entity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                getRootUrl() + "/auth/logout", HttpMethod.PUT, entity, Void.class
        );

        assertNotNull(response);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    protected ClaimDto createSingleClaim(final HttpHeaders headers,
                                         final String phone,
                                         final String request,
                                         final HttpStatus status) {

        final CreateClaimRequest createClaimRequest = new CreateClaimRequest(
                phone,
                request
        );

        final HttpEntity<CreateClaimRequest> entity = new HttpEntity<>(createClaimRequest, headers);
        final ResponseEntity<ClaimDto> getResponse = restTemplate.exchange(
                getRootUrl() + "/claim", HttpMethod.POST, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals(status, getResponse.getStatusCode());
        return getResponse.getBody();
    }

}
