package com.customerservice;

import com.customerservice.domain.ERequestStatus;
import com.customerservice.domain.ERole;
import com.customerservice.domain.PhoneInfo;
import com.customerservice.rest.domain.ClaimDto;
import com.customerservice.rest.domain.UpdateUserRoleRequest;
import com.customerservice.rest.domain.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CustomerServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserTests extends BaseTest {

    @Test
    public void createAndDeleteUser() {
        final UserDto user = userService.createUser(USER2_NAME, Set.of(ERole.USER), USER_PASSWORD);
        final ClaimDto claim = claimService.createClaim(
                user.getId(),
                ERequestStatus.DRAFT,
                new PhoneInfo("+70000000"),
                "request: " + UUID.randomUUID(),
                Date.from(Instant.now())
        );
        assertNotNull(userService.getUserById(user.getId()));
        assertNotNull(claimService.getClaimById(claim.getId()));

        userService.deleteUserById(user.getId());
        assertNull(userService.getUserById(user.getId()));
        assertNull(claimService.getClaimById(claim.getId()));
    }

    @Test
    public void testSingleRoleUser() {

        final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);

        // Check existing ROLES
        int i = jwt.lastIndexOf('.');
        String withoutSignature = jwt.substring(0, i+1);
        Jwt<Header, Claims> untrusted = Jwts.parser().parseClaimsJwt(withoutSignature);

        assertEquals(untrusted.getBody().getSubject(), USER_NAME);
        List<String> roles = untrusted.getBody().get("roles", ArrayList.class);
        assertEquals(roles.toString(), List.of(ERole.USER).toString());

        logoutByJwt(jwt);
    }

    @Test
    public void testMultiRoleUser() {

        final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);

        // Check existing ROLES
        int i = jwt.lastIndexOf('.');
        String withoutSignature = jwt.substring(0, i+1);
        Jwt<Header, Claims> untrusted = Jwts.parser().parseClaimsJwt(withoutSignature);

        assertEquals(untrusted.getBody().getSubject(), OPERATOR_NAME);
        List<String> roles = untrusted.getBody().get("roles", ArrayList.class);

        assertEquals(Arrays.asList(ERole.OPERATOR, ERole.USER).toString(), roles.toString());

        logoutByJwt(jwt);
    }

    private void setUserRole(final HttpHeaders headers,
                             final String username,
                             final ERole role,
                             final boolean enable,
                             final HttpStatus expectedStatus) {
        // Add OPERATOR ROLE
        UpdateUserRoleRequest updateUserRoleRequest = new UpdateUserRoleRequest(
                role,
                enable
        );

        HttpEntity<UpdateUserRoleRequest> entity = new HttpEntity<>(updateUserRoleRequest, headers);
        ResponseEntity<?> putResponse = restTemplate.exchange(
                getRootUrl() + "/user/role/" + username, HttpMethod.PUT, entity, UpdateUserRoleRequest.class
        );
        assertNotNull(putResponse);
        assertEquals(putResponse.getStatusCode(), expectedStatus);
    }

    @Test
    public void setUserRoleToOperatorByAdmin() {

        final String jwt = authUserGetJwt(ADMIN_NAME, ADMIN_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);

        UserDto user = userService.getUserByName(USER_NAME);
        assertEquals(Set.of(ERole.USER), user.getRoles());

        // Add OPERATOR ROLE
        setUserRole(headers, USER_NAME, ERole.OPERATOR, true, HttpStatus.OK);
        user = userService.getUserByName(USER_NAME);
        assertEquals(Set.of(ERole.USER, ERole.OPERATOR), user.getRoles());

        // Remove OPERATOR ROLE
        setUserRole(headers, USER_NAME, ERole.OPERATOR, false, HttpStatus.OK);
        user = userService.getUserByName(USER_NAME);
        assertEquals(Set.of(ERole.USER), user.getRoles());
    }

    @Test
    public void setOperatorRoleByUser() {

        final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);

        // Add OPERATOR ROLE
        setUserRole(headers, USER_NAME, ERole.OPERATOR, true, HttpStatus.FORBIDDEN);
    }

    @Test
    public void setOperatorRoleByOperator() {

        final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);

        // Add OPERATOR ROLE
        setUserRole(headers, USER_NAME, ERole.OPERATOR, true, HttpStatus.FORBIDDEN);
    }

    @Test
    public void setNotAllowedOperatorRoleByUser() {
        final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);

        // Add USER ROLE
        setUserRole(headers, USER_NAME, ERole.USER, true, HttpStatus.FORBIDDEN);

        // Add OPERATOR ROLE
        setUserRole(headers, USER_NAME, ERole.OPERATOR, true, HttpStatus.FORBIDDEN);

        // Add ADMIN ROLE
        setUserRole(headers, USER_NAME, ERole.ADMIN, true, HttpStatus.FORBIDDEN);
    }

    @Test
    public void setNotAllowedOperatorRoleByOperator() {
        final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);

        // Add USER ROLE
        setUserRole(headers, USER_NAME, ERole.USER, true, HttpStatus.FORBIDDEN);

        // Add OPERATOR ROLE
        setUserRole(headers, USER_NAME, ERole.OPERATOR, true, HttpStatus.FORBIDDEN);

        // Add ADMIN ROLE
        setUserRole(headers, USER_NAME, ERole.ADMIN, true, HttpStatus.FORBIDDEN);
    }

    @Test
    public void setNotAllowedOperatorRoleByAdmin() {
        final String jwt = authUserGetJwt(ADMIN_NAME, ADMIN_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);

        // Add USER ROLE
        setUserRole(headers, USER_NAME, ERole.USER, true, HttpStatus.FORBIDDEN);

        // Add ADMIN ROLE
        setUserRole(headers, USER_NAME, ERole.ADMIN, true, HttpStatus.FORBIDDEN);
    }

    @Test
    public void getAllUsers() {

        final String jwt = authUserGetJwt(ADMIN_NAME, ADMIN_PASSWORD);
        final HttpHeaders headers = getHeaderWithJwt(jwt);
        final HttpEntity<UpdateUserRoleRequest> entity = new HttpEntity<>(headers);

        ResponseEntity<List<UserDto>> getResponse = restTemplate.exchange(
                getRootUrl() + "/user", HttpMethod.GET, entity,
                new ParameterizedTypeReference<>() {}
        );
        assertNotNull(getResponse);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());

        List<UserDto> result = getResponse.getBody();
        List<UserDto> actual = userService.findAll().stream()
                .map(userService::convertToDto)
                .toList();

        assertTrue(result.size() > 0);
        assertEquals(actual.size(), result.size());
        assertEquals(result, actual);
    }

    @Test
    @Disabled
    public void createUsers() {
        userService.createUser(USER_NAME, Set.of(ERole.USER), USER_PASSWORD);
        userService.createUser(OPERATOR_NAME, Set.of(ERole.USER, ERole.OPERATOR), OPERATOR_PASSWORD);
        userService.createUser(ADMIN_NAME, Set.of(ERole.USER, ERole.ADMIN), ADMIN_PASSWORD);
    }

    @Test
    @Disabled
    public void deleteAllUsers() {
        userService.deleteAllUsers();
    }

}
