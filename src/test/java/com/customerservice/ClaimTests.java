package com.customerservice;

import com.customerservice.domain.ERequestStatus;
import com.customerservice.domain.PhoneInfo;
import com.customerservice.rest.domain.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CustomerServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ClaimTests extends BaseTest {

    @Test
    public void createClaim() throws Exception {

        final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);

        final String request = UUID.randomUUID().toString();
        ClaimDto claim = createSingleClaim(headers, USER_PHONE, request, HttpStatus.OK);
        assertEquals(claimService.getClaimById(claim.getId()), claim);
        assertEquals(claim.getStatus(), ERequestStatus.DRAFT);
        assertNotNull(claim.getCreatedAt());
        assertEquals(USER_PHONE, claim.getPhone().getPhoneNumber());
        assertEquals(claim.getRequest(), request);
        long claimId = claim.getId();
        waitForCondition(1_000,
                500,
                () -> claimService.getClaimById(claimId).getPhone().getCity() != null
        );
        claimService.deleteClaimById(claim.getId());
    }

    @Test
    public void createClaimWithInvalidData() {
        final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);

        // Test wrong phone
        createSingleClaim(headers, null, UUID.randomUUID().toString(), HttpStatus.BAD_REQUEST);
    }

    @Test
    public void updateClaimByUser() {

        if (claimService.getAllCount() <= MAX_TEST_CLAIMS_COUNT) {
            createClaimsForTests();
        }

        final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);
        final UserDto user = userService.getUserByName(USER_NAME);

        ClaimDto actualClaim = claimService.findAll().stream()
                .filter(entry ->
                        entry.getUserId() == user.getId()
                                && entry.getStatus().equals(ERequestStatus.DRAFT))
                .findFirst().orElse(null);
        assertNotNull(actualClaim);

        final String text = UUID.randomUUID().toString();
        UpdateClaimRequest updateClaimRequest = new UpdateClaimRequest(
                USER_PHONE + String.valueOf(new Random().nextInt()).substring(0, 5),
                text
        );

        HttpEntity<UpdateClaimRequest> entity = new HttpEntity<>(updateClaimRequest, headers);
        ResponseEntity<ClaimDto> getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/" + actualClaim.getId(), HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());

        actualClaim = claimService.findAll().stream()
                .filter(entry -> entry.getUserId() == user.getId()
                        && entry.getRequest().equals(text))
                .findFirst().orElse(null);
        assertNotNull(actualClaim);


        // Test wrong Id
        updateClaimRequest = new UpdateClaimRequest(
                USER_PHONE + new Random().nextInt(),
                text
        );

        entity = new HttpEntity<>(updateClaimRequest, headers);
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/" + 9999999, HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());

        // Test wrong phone
        updateClaimRequest = new UpdateClaimRequest(
                null,
                text
        );

        entity = new HttpEntity<>(updateClaimRequest, headers);
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/" + actualClaim.getId(), HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

    }

    @Test
    public void updateClaimStatusByUser() {

        final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);
        final UserDto user = userService.getUserByName(USER_NAME);

        ClaimDto actualClaim = claimService.findAll().stream()
                .filter(entry ->
                        entry.getUserId() == user.getId()
                                && entry.getStatus().equals(ERequestStatus.DRAFT))
                .findFirst().orElse(null);
        assertNotNull(actualClaim);

        // Wrong DRAFT
        HttpEntity<ChangeClaimStatusRequest> entity = new HttpEntity<>(
                new ChangeClaimStatusRequest(
                        ERequestStatus.DRAFT
                ),
                headers
        );
        ResponseEntity<ClaimDto> getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/status/" + actualClaim.getId(), HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals( HttpStatus.FORBIDDEN, getResponse.getStatusCode());

        // Wrong RECEIVED
        entity = new HttpEntity<>(
                new ChangeClaimStatusRequest(
                        ERequestStatus.RECEIVED
                ),
                headers
        );
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/status/" + actualClaim.getId(), HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals( HttpStatus.FORBIDDEN, getResponse.getStatusCode());

        // Wrong REJECTED
        entity = new HttpEntity<>(
                new ChangeClaimStatusRequest(
                        ERequestStatus.REJECTED
                ),
                headers
        );
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/status/" + actualClaim.getId(), HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals( HttpStatus.FORBIDDEN, getResponse.getStatusCode());

        // CORRECT SENT
        entity = new HttpEntity<>(
                new ChangeClaimStatusRequest(
                        ERequestStatus.SENT
                ),
                headers
        );
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/status/" + actualClaim.getId(), HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals( HttpStatus.OK, getResponse.getStatusCode());

        final long testId = actualClaim.getId();
        actualClaim = claimService.findAll().stream()
                .filter(entry -> entry.getId() == testId)
                .findFirst().orElse(null);
        assertNotNull(actualClaim);
        assertEquals(actualClaim.getStatus(), ERequestStatus.SENT);
    }

    @Test
    public void updateClaimStatusByOperator() {

        if (claimService.getAllCount() <= MAX_TEST_CLAIMS_COUNT) {
            createClaimsForTests();
        }

        final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);

        ClaimDto actualClaim = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
                .findFirst().orElse(null);
        assertNotNull(actualClaim);

        // Wrong DRAFT
        HttpEntity<ChangeClaimStatusRequest> entity = new HttpEntity<>(
                new ChangeClaimStatusRequest(
                        ERequestStatus.DRAFT
                ),
                headers
        );
        ResponseEntity<ClaimDto> getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/status/" + actualClaim.getId(), HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals( HttpStatus.FORBIDDEN, getResponse.getStatusCode());

        // Wrong SENT
        entity = new HttpEntity<>(
                new ChangeClaimStatusRequest(
                        ERequestStatus.SENT
                ),
                headers
        );
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/status/" + actualClaim.getId(), HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals( HttpStatus.FORBIDDEN, getResponse.getStatusCode());

        // CORRECT REJECTED
        entity = new HttpEntity<>(
                new ChangeClaimStatusRequest(
                        ERequestStatus.REJECTED
                ),
                headers
        );
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/status/" + actualClaim.getId(), HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals( HttpStatus.OK, getResponse.getStatusCode());

        final long id = actualClaim.getId();
        actualClaim = claimService.findAll().stream()
                .filter(entry -> entry.getId() == id)
                .findFirst().orElse(null);
        assertNotNull(actualClaim);
        assertEquals(actualClaim.getStatus(), ERequestStatus.REJECTED);


        // Find new SENT claim
        actualClaim = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
                .findFirst().orElse(null);

        // CORRECT RECEIVED
        entity = new HttpEntity<>(
                new ChangeClaimStatusRequest(
                        ERequestStatus.RECEIVED
                ),
                headers
        );
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/status/" + actualClaim.getId(), HttpMethod.PUT, entity, ClaimDto.class
        );
        assertNotNull(getResponse);
        assertEquals( HttpStatus.OK, getResponse.getStatusCode());

        final long id2 = actualClaim.getId();
        actualClaim = claimService.findAll().stream()
                .filter(entry -> entry.getId() == id2)
                .findFirst().orElse(null);
        assertNotNull(actualClaim);
        assertEquals(actualClaim.getStatus(), ERequestStatus.RECEIVED);

    }

    @Test
    public void getClaimsByUserWithErrors() {

        final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);
        final HttpEntity<UpdateUserRoleRequest> entity = new HttpEntity<>(headers);

        // Wrong sort
        ResponseEntity<List<ClaimDto>> getResponse = restTemplate.exchange(
                getRootUrl() + "/claim?sort=X",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        assertNotNull(getResponse);
        assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

        // Wrong from
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim?from=-1",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        assertNotNull(getResponse);
        assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());
    }

    private List<ClaimDto> getClaims(final HttpHeaders headers,
                                     final String params,
                                     final HttpStatus status) {

        final HttpEntity<UpdateUserRoleRequest> entity = new HttpEntity<>(headers);

        // Check DEFAULT mode, SORT=DESC, FROM=0
        ResponseEntity<List<ClaimDto>> getResponse = restTemplate.exchange(
                getRootUrl() + "/claim" + params,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        assertNotNull(getResponse);
        assertEquals(getResponse.getStatusCode(), status);

        if (status == HttpStatus.OK) {
            return getResponse.getBody();
        }

        return null;
    }

    @Test
    public void getClaimsByUser() {

        if (claimService.getAllCount() <= MAX_TEST_CLAIMS_COUNT) {
            createClaimsForTests();
        }

        final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);
        final UserDto user = userService.getUserByName(USER_NAME);

        // Check DEFAULT mode, SORT=DESC, FROM=0
        List<ClaimDto> result = getClaims(headers, "", HttpStatus.OK);

        List<ClaimDto> actual = claimService.findAll().stream()
                .filter(entry -> entry.getUserId() == user.getId())
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt).reversed())
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertTrue(result.equals(actual));

        // Check ASC mode
        result = getClaims(headers, "?sort=ASC", HttpStatus.OK);

        actual = claimService.findAll().stream()
                .filter(entry -> entry.getUserId() == user.getId())
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt))
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertTrue(result.equals(actual));

        // Check ASC mode, FROM = 2 (2nd page)
        result = getClaims(headers, "?sort=ASC&from=2", HttpStatus.OK);

        actual = claimService.findAll().stream()
                .filter(entry -> entry.getUserId() == user.getId())
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt))
                .skip(2*PAGE_SIZE)
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertTrue(result.equals(actual));

        // Check DESC mode, FROM = 1 (1st page)
        result = getClaims(headers, "?sort=DESC&from=1", HttpStatus.OK);

        actual = claimService.findAll().stream()
                .filter(entry -> entry.getUserId() == user.getId())
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt).reversed())
                .skip(1*PAGE_SIZE)
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);

    }

    @Test
    public void getClaimsByOperator() {

        final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);
        final UserDto user = userService.getUserByName(USER_NAME);

        // Check ANY USER, SORT=DESC, FROM=0
        List<ClaimDto> result = getClaims(headers, "/operator", HttpStatus.OK);

        List<ClaimDto> actual = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt).reversed())
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);

        // Check USER use* , SORT=DESC, FROM=0
        result = getClaims(headers, "/operator?username=use", HttpStatus.OK);

        actual = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT)
                        && entry.getUserId() == user.getId())
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt).reversed())
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);

        // Check ASC mode
        result = getClaims(headers, "/operator?sort=ASC", HttpStatus.OK);

        actual = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt))
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);

        // Check ASC mode, FROM = 2 (2nd page)
        result = getClaims(headers, "/operator?sort=ASC&from=2", HttpStatus.OK);

        actual = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt))
                .skip(2 * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);

        // Check DESC mode, FROM = 1 (1st page)
        result = getClaims(headers, "/operator?sort=DESC&from=1", HttpStatus.OK);

        actual = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt).reversed())
                .skip(1 * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);

    }

    @Test
    public void getSingleClaimByOperator() {

        if (claimService.getAllCount() <= MAX_TEST_CLAIMS_COUNT) {
            createClaimsForTests();
        }

        final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);
        final HttpEntity entity = new HttpEntity<>(headers);

        final ClaimDto actualClaim = claimService.findAll().stream()
                .filter(e -> e.getStatus().equals(ERequestStatus.SENT))
                .skip(2)
                .findFirst()
                .orElse(null);
        assertNotNull(actualClaim);

        // Check One claim by ID
        ResponseEntity<ClaimDto>  getSingleResponse = restTemplate.exchange(
                getRootUrl() + "/claim/operator/" + actualClaim.getId(),
                HttpMethod.GET, entity, ClaimDto.class
        );
        assertNotNull(getSingleResponse);
        assertEquals(getSingleResponse.getStatusCode(), HttpStatus.OK);
        ClaimDto singleClaim = getSingleResponse.getBody();
        assertEquals(singleClaim, actualClaim);

        ResponseEntity<?> getSingleResponse2 = restTemplate.exchange(
                getRootUrl() + "/claim/operator/" + -2,
                HttpMethod.GET, entity, Void.class
        );
        assertNotNull(getSingleResponse2);
        assertEquals(HttpStatus.BAD_REQUEST, getSingleResponse2.getStatusCode());

        getSingleResponse2 = restTemplate.exchange(
                getRootUrl() + "/claim/operator/" + 99999999,
                HttpMethod.GET, entity, Void.class
        );
        assertNotNull(getSingleResponse2);
        assertEquals(HttpStatus.NOT_FOUND, getSingleResponse2.getStatusCode());
    }

    @Test
    public void getClaimsErrorsByOperator() {

        final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);
        final HttpEntity<UpdateUserRoleRequest> entity = new HttpEntity<>(headers);

        // Wrong sort
        ResponseEntity<List<ClaimDto>> getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/operator?sort=X",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        assertNotNull(getResponse);
        assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

        // Wrong from
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/operator?from=-1",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        assertNotNull(getResponse);
        assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());
    }


    @Test
    public void getAdminClaimsErrors() {

        final String jwt = authUserGetJwt(ADMIN_NAME, ADMIN_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);
        final HttpEntity<UpdateUserRoleRequest> entity = new HttpEntity<>(headers);

        // Wrong sort
        ResponseEntity<List<ClaimDto>> getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/admin?sort=X",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        assertNotNull(getResponse);
        assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

        // Wrong from
        getResponse = restTemplate.exchange(
                getRootUrl() + "/claim/admin?from=-1",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        assertNotNull(getResponse);
        assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());
    }

    @Test
    public void getClaimsByAdmin() {

        if (claimService.getAllCount() <= MAX_TEST_CLAIMS_COUNT) {
            createClaimsForTests();
        }

        final String jwt = authUserGetJwt(ADMIN_NAME, ADMIN_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);

        // Check SORT=DESC, FROM=0
        List<ClaimDto> result = getClaims(headers, "/admin", HttpStatus.OK);

        List<ClaimDto> actual = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT)
                        || entry.getStatus().equals(ERequestStatus.REJECTED)
                        || entry.getStatus().equals(ERequestStatus.RECEIVED))
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt).reversed())
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);

        // Check SORT=DESC, FROM=2
        result = getClaims(headers, "/admin?from=2", HttpStatus.OK);

        actual = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT)
                        || entry.getStatus().equals(ERequestStatus.REJECTED)
                        || entry.getStatus().equals(ERequestStatus.RECEIVED))
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt).reversed())
                .skip(2*PAGE_SIZE)
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);

        // Check SORT=ASC, FROM=0
        result = getClaims(headers, "/admin?sort=ASC", HttpStatus.OK);

        actual = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT)
                        || entry.getStatus().equals(ERequestStatus.REJECTED)
                        || entry.getStatus().equals(ERequestStatus.RECEIVED))
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt))
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);

        // Check SORT=ASC, FROM=1
        result = getClaims(headers, "/admin?sort=ASC&from=1", HttpStatus.OK);

        actual = claimService.findAll().stream()
                .filter(entry -> entry.getStatus().equals(ERequestStatus.SENT)
                        || entry.getStatus().equals(ERequestStatus.REJECTED)
                        || entry.getStatus().equals(ERequestStatus.RECEIVED))
                .sorted(Comparator.comparing(ClaimDto::getCreatedAt))
                .skip(1*PAGE_SIZE)
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);
    }

    @Test
    @Disabled
    void createTestClaims() {
        createClaimsForTests();
    }

    private void createClaimsForTests() {

        final UserDto user = userService.getUserByName(USER_NAME);
        assertNotNull(user);

        for (int i = 0; i < MAX_TEST_CLAIMS_COUNT; i++) {
            claimService.createClaim(
                    user.getId(),
                    ERequestStatus.DRAFT,
                    new PhoneInfo("+7111" + i),
                    "request: " + UUID.randomUUID(),
                    Date.from(Instant.now())
            );

            claimService.createClaim(
                    user.getId(),
                    ERequestStatus.SENT,
                    new PhoneInfo("+7222" + i),
                    "request: " + UUID.randomUUID(),
                    Date.from(Instant.now())
            );

            claimService.createClaim(
                    user.getId(),
                    ERequestStatus.RECEIVED,
                    new PhoneInfo("+7333" + i),
                    "request: " + UUID.randomUUID(),
                    Date.from(Instant.now())
            );

            claimService.createClaim(
                    user.getId(),
                    ERequestStatus.REJECTED,
                    new PhoneInfo("+7444" + i),
                    "request: " + UUID.randomUUID(),
                    Date.from(Instant.now())
            );
        }
    }

    public static void waitForCondition(final long maxWaitMs,
                                        final long sleepWaitMs,
                                        final Callable<Boolean> checkCondition) {
        final long startTimeMs = System.currentTimeMillis();
        assertDoesNotThrow(() -> {
            boolean condition = false;
            while (!condition) {
                Thread.sleep(sleepWaitMs);
                if (System.currentTimeMillis() - startTimeMs >= maxWaitMs) {
                    System.out.println("Wait condition failed, exited by timeout");
                    break;
                }
                condition = checkCondition.call();
            }},
                "Exception happens during wait."
        );
    }
}
