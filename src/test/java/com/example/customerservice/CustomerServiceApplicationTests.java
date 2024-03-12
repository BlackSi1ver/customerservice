package com.example.customerservice;

import com.example.customerservice.auth.AuthRequest;
import com.example.customerservice.auth.AuthResponse;
import com.example.customerservice.auth.JwtUtil;
import com.example.customerservice.data.*;
import com.example.customerservice.repositories.ClaimsRepository;
import com.example.customerservice.repositories.UsersRepository;
import com.example.customerservice.requests.ChangeClaimStatusRequest;
import com.example.customerservice.requests.CreateClaimRequest;
import com.example.customerservice.requests.UpdateClaimRequest;
import com.example.customerservice.requests.UpdateRolesRequest;
import com.example.customerservice.servicies.ClaimService;
import com.example.customerservice.servicies.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = CustomerServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerServiceApplicationTests {

	static final String USER_NAME = "user1";
	static final String USER_PASSWORD = "userpass";
	static final String USER_PHONE = "+79147904444";
	static final String OPERATOR_NAME = "oper1";
	static final String OPERATOR_PASSWORD = "operpass";
	static final String ADMIN_NAME = "admin1";
	static final String ADMIN_PASSWORD = "adminpass";

	final int MAX_TEST_CLAIMS_COUNT = 80;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private ClaimsRepository claimsRepository;

	@Autowired
	private ClaimService claimService;

	@Autowired
	private JwtUtil jwtUtil;

	@LocalServerPort
	private int port;

	private String getRootUrl() {
		return "http://localhost:" + port + "/api/v1";
	}


	@Test
	void testSingleRoleUser() {

		final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);

		// Check existing ROLES
		int i = jwt.lastIndexOf('.');
		String withoutSignature = jwt.substring(0, i+1);
		Jwt<Header, Claims> untrusted = Jwts.parser().parseClaimsJwt(withoutSignature);

		assertEquals(untrusted.getBody().getSubject(), USER_NAME);
		List<String> roles = untrusted.getBody().get("roles", ArrayList.class);
		assertEquals(roles.toString(), Arrays.asList(ERole.USER).toString());

		logoutByJwt(jwt);
	}

	@Test
	void testMultiRoleUser() {

		final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);

		int i = jwt.lastIndexOf('.');
		String withoutSignature = jwt.substring(0, i+1);
		Jwt<Header, Claims> untrusted = Jwts.parser().parseClaimsJwt(withoutSignature);

		assertEquals(untrusted.getBody().getSubject(), OPERATOR_NAME);
		List<String> roles = untrusted.getBody().get("roles", ArrayList.class);

		assertEquals(Arrays.asList(ERole.OPERATOR, ERole.USER).toString(), roles.toString());

		logoutByJwt(jwt);
	}

	@Test
	void setOperatorRoleByAdmin() {

		final String jwt = authUserGetJwt(ADMIN_NAME, ADMIN_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);

		// Add OPERATOR ROLE
		UpdateRolesRequest updateRolesRequest = new UpdateRolesRequest(
				USER_NAME,
				true
		);

		HttpEntity<UpdateRolesRequest> entity = new HttpEntity<UpdateRolesRequest>(updateRolesRequest, headers);
		ResponseEntity<?> putResponse = restTemplate.exchange(
				getRootUrl() + "/users/roles", HttpMethod.PUT, entity, UpdateRolesRequest.class
		);
		assertNotNull(putResponse);
		assertEquals(putResponse.getStatusCode(), HttpStatus.OK);

		UserEntry user = usersRepository.findByName(USER_NAME).get();
		assertEquals(Set.of(ERole.USER, ERole.OPERATOR), user.getRoles());

		// Remove OPERATOR ROLE
		updateRolesRequest = new UpdateRolesRequest(
				USER_NAME,
				false
		);

		entity = new HttpEntity<UpdateRolesRequest>(updateRolesRequest, headers);
		putResponse = restTemplate.exchange(
				getRootUrl() + "/users/roles", HttpMethod.PUT, entity, UpdateRolesRequest.class
		);
		assertNotNull(putResponse);
		assertEquals(HttpStatus.OK, putResponse.getStatusCode());

		user = usersRepository.findByName(USER_NAME).get();
		assertEquals(Set.of(ERole.USER), user.getRoles());
	}

	@Test
	void setOperatorRoleByUser() {

		final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);

		// Add OPERATOR ROLE
		UpdateRolesRequest updateRolesRequest = new UpdateRolesRequest(
				USER_NAME,
				true
		);

		HttpEntity<UpdateRolesRequest> entity = new HttpEntity<UpdateRolesRequest>(updateRolesRequest, headers);
		ResponseEntity<?> putResponse = restTemplate.exchange(
				getRootUrl() + "/users/roles", HttpMethod.PUT, entity, UpdateRolesRequest.class
		);
		assertNotNull(putResponse);
		assertEquals(HttpStatus.FORBIDDEN, putResponse.getStatusCode());
	}

	@Test
	void setOperatorRoleByOperator() {

		final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);

		// Add OPERATOR ROLE
		UpdateRolesRequest updateRolesRequest = new UpdateRolesRequest(
				USER_NAME,
				true
		);

		HttpEntity<UpdateRolesRequest> entity = new HttpEntity<UpdateRolesRequest>(updateRolesRequest, headers);
		ResponseEntity<?> putResponse = restTemplate.exchange(
				getRootUrl() + "/users/roles", HttpMethod.PUT, entity, UpdateRolesRequest.class
		);
		assertNotNull(putResponse);
		assertEquals(putResponse.getStatusCode(), HttpStatus.FORBIDDEN);
	}

	@Test
	public void getAllUsers() {

		final String jwt = authUserGetJwt(ADMIN_NAME, ADMIN_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);
		final HttpEntity<UpdateRolesRequest> entity = new HttpEntity<>(headers);

		ResponseEntity<List<UserDto>> getResponse = restTemplate.exchange(
				getRootUrl() + "/users", HttpMethod.GET, entity,
				new ParameterizedTypeReference<List<UserDto>>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		List<UserDto> result = getResponse.getBody();
		List<UserDto> actual = usersRepository.findAll().stream()
				.map(userService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));
	}

	@Test
	public void createUserClaim() {

		final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);

		String text = UUID.randomUUID().toString();
		CreateClaimRequest createClaimRequest = new CreateClaimRequest(
				USER_PHONE,
				text
		);

		HttpEntity<CreateClaimRequest> entity = new HttpEntity<>(createClaimRequest, headers);
		ResponseEntity<?> getResponse = restTemplate.exchange(
				getRootUrl() + "/claims", HttpMethod.POST, entity, ResponseEntity.class
		);
		assertNotNull(getResponse);
		assertEquals(HttpStatus.OK, getResponse.getStatusCode());

		List<ClaimEntry> actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getUser().getName().equals(USER_NAME)
						&& entry.getRequest().equals(text))
				.limit(1)
				.collect(Collectors.toList());

		assertEquals(actual.size(), 1);

		// Test wrong phone
		createClaimRequest = new CreateClaimRequest(
				null,
				text
		);
		entity = new HttpEntity<>(createClaimRequest, headers);
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims", HttpMethod.POST, entity, ResponseEntity.class
		);
		assertNotNull(getResponse);
		assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());
	}

	@Test
	public void updateUserClaim() {

		final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);

		List<ClaimEntry> actual = claimsRepository.findAll().stream()
				.filter(entry ->
						entry.getUser().getName().equals(USER_NAME)
								&& entry.getStatus().equals(ERequestStatus.DRAFT))
				.limit(1)
				.collect(Collectors.toList());

		final String text = UUID.randomUUID().toString();
		UpdateClaimRequest updateClaimRequest = new UpdateClaimRequest(
				actual.get(0).getId(),
				USER_PHONE + String.valueOf(new Random().nextInt()).substring(0, 5),
				text
		);

		HttpEntity<UpdateClaimRequest> entity = new HttpEntity<>(updateClaimRequest, headers);
		ResponseEntity<Void> getResponse = restTemplate.exchange(
				getRootUrl() + "/claims", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getUser().getName().equals(USER_NAME)
						&& entry.getRequest().equals(text))
				.limit(1)
				.collect(Collectors.toList());
		assertEquals(actual.size(), 1);


		// Test wrong Id
		updateClaimRequest = new UpdateClaimRequest(
				actual.get(0).getId() + 9999999,
				USER_PHONE + new Random().nextInt(),
				text
		);

		entity = new HttpEntity<>(updateClaimRequest, headers);
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

		// Test wrong phone
		updateClaimRequest = new UpdateClaimRequest(
				actual.get(0).getId(),
				null,
				text
		);

		entity = new HttpEntity<>(updateClaimRequest, headers);
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

	}

	@Test
	public void updateUserStatus() {

		final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);

		List<ClaimEntry> actual = claimsRepository.findAll().stream()
				.filter(entry ->
						entry.getUser().getName().equals(USER_NAME)
								&& entry.getStatus().equals(ERequestStatus.DRAFT))
				.limit(1)
				.collect(Collectors.toList());
		assertEquals(actual.size(), 1);

		// Wrong DRAFT
		HttpEntity<ChangeClaimStatusRequest> entity = new HttpEntity<>(
				new ChangeClaimStatusRequest(
						actual.get(0).getId(),
						ERequestStatus.DRAFT
				),
				headers
		);
		ResponseEntity<Void> getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/status", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals( HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

		// Wrong RECEIVED
		entity = new HttpEntity<>(
				new ChangeClaimStatusRequest(
						actual.get(0).getId(),
						ERequestStatus.RECEIVED
				),
				headers
		);
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/status", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals( HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

		// Wrong REJECTED
		entity = new HttpEntity<>(
				new ChangeClaimStatusRequest(
						actual.get(0).getId(),
						ERequestStatus.REJECTED
				),
				headers
		);
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/status", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals( HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

		// CORRECT SENT
		long testId = actual.get(0).getId();
		entity = new HttpEntity<>(
				new ChangeClaimStatusRequest(
						testId,
						ERequestStatus.SENT
				),
				headers
		);
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/status", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals( HttpStatus.OK, getResponse.getStatusCode());

		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getId() == testId)
				.collect(Collectors.toList());
		assertTrue(actual.size() == 1);
	}

	@Test
	public void updateOperatorStatus() {

		final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);

		List<ClaimEntry> actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
				.limit(1)
				.collect(Collectors.toList());

		// Wrong DRAFT
		HttpEntity<ChangeClaimStatusRequest> entity = new HttpEntity<>(
				new ChangeClaimStatusRequest(
						actual.get(0).getId(),
						ERequestStatus.DRAFT
				),
				headers
		);
		ResponseEntity<Void> getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/status", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals( HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

		// Wrong RECEIVED
		entity = new HttpEntity<>(
				new ChangeClaimStatusRequest(
						actual.get(0).getId(),
						ERequestStatus.SENT
				),
				headers
		);
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/status", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals( HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

		// CORRECT REJECTED
		entity = new HttpEntity<>(
				new ChangeClaimStatusRequest(
						actual.get(0).getId(),
						ERequestStatus.REJECTED
				),
				headers
		);
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/status", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals( HttpStatus.OK, getResponse.getStatusCode());

		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
				.limit(1)
				.collect(Collectors.toList());

		// CORRECT RECEIVED
		long testId = actual.get(0).getId();
		entity = new HttpEntity<>(
				new ChangeClaimStatusRequest(
						testId,
						ERequestStatus.RECEIVED
				),
				headers
		);
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/status", HttpMethod.PUT, entity, Void.class
		);
		assertNotNull(getResponse);
		assertEquals( HttpStatus.OK, getResponse.getStatusCode());

		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getId() == testId)
				.collect(Collectors.toList());
		assertTrue(actual.size() == 1);
	}

	@Test
	public void getUsersClaimsErrors() {

		final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);
		final HttpEntity<UpdateRolesRequest> entity = new HttpEntity<>(headers);

		// Wrong sort
		ResponseEntity<List<ClaimEntry>> getResponse = restTemplate.exchange(
					getRootUrl() + "/claims?sort=X",
					HttpMethod.GET,
					entity,
				new ParameterizedTypeReference<List<ClaimEntry>>() {}
			);
		assertNotNull(getResponse);
		assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

		// Wrong from
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims?from=-1",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<List<ClaimEntry>>() {}
		);
		assertNotNull(getResponse);
		assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());
	}

	@Test
	public void getUsersClaims() {

		if (claimsRepository.findAll().size() <= MAX_TEST_CLAIMS_COUNT/ 4) {
			createClaimsForTests();
		}

		final String jwt = authUserGetJwt(USER_NAME, USER_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);
		final HttpEntity<UpdateRolesRequest> entity = new HttpEntity<>(headers);

		// Check DEFAULT mode, SORT=DESC, FROM=0
		ResponseEntity<List<ClaimDto>> getResponse = restTemplate.exchange(
				getRootUrl() + "/claims",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		List<ClaimDto> result = getResponse.getBody();
		List<ClaimDto> actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getUser().getName().equals(USER_NAME))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt).reversed())
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));
		assertTrue(result.stream().allMatch(dto -> dto.getUsername().equals(USER_NAME)));

		// Check ASC mode
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims" + "?sort=ASC",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		result = getResponse.getBody();
		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getUser().getName().equals(USER_NAME))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt))
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));

		// Check ASC mode, FROM = 2 (2nd page)
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims" + "?sort=ASC&from=2",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		result = getResponse.getBody();
		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getUser().getName().equals(USER_NAME))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt))
				.skip(2*5)
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));

		// Check DESC mode, FROM = 1 (1st page)
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims" + "?sort=DESC&from=1",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		result = getResponse.getBody();
		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getUser().getName().equals(USER_NAME))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt).reversed())
				.skip(1*5)
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));

	}

	@Test
	public void getOperatorClaimsErrors() {

		final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);
		final HttpEntity<UpdateRolesRequest> entity = new HttpEntity<>(headers);

		// Wrong sort
		ResponseEntity<List<ClaimEntry>> getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/operator?sort=X",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<List<ClaimEntry>>() {}
		);
		assertNotNull(getResponse);
		assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

		// Wrong from
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/operator?from=-1",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<List<ClaimEntry>>() {}
		);
		assertNotNull(getResponse);
		assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());
	}

	@Test
	public void getOperatorClaims() {

		final String jwt = authUserGetJwt(OPERATOR_NAME, OPERATOR_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);
		final HttpEntity entity = new HttpEntity<>(headers);

		// Check ANY USER, SORT=DESC, FROM=0
		ResponseEntity<List<ClaimDto>> getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/operator",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		List<ClaimDto> result = getResponse.getBody();
		List<ClaimDto> actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt).reversed())
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));

		// Check USER user2* , SORT=DESC, FROM=0
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/operator?username=" + "use",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		result = getResponse.getBody();
		 actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT)
						&& entry.getUser().getName().contains("use"))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt).reversed())
				.limit(5)
				 .map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));

		// Check ASC mode
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/operator" + "?sort=ASC",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		result = getResponse.getBody();
		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt))
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));

		// Check ASC mode, FROM = 2 (2nd page)
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/operator" + "?sort=ASC&from=2",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		result = getResponse.getBody();
		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt))
				.skip(2*5)
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));

		// Check DESC mode, FROM = 1 (1st page)
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/operator" + "?sort=DESC&from=1",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		result = getResponse.getBody();
		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt).reversed())
				.skip(1*5)
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));


		// Check One claim by ID
		assertTrue(actual.size() > 1);
		ResponseEntity<ClaimDto>  getSingleResponse = restTemplate.exchange(
				getRootUrl() + "/claims/operator/" + actual.get(0).getId(),
				HttpMethod.GET, entity, ClaimDto.class
		);
		assertNotNull(getSingleResponse);
		assertEquals(getSingleResponse.getStatusCode(), HttpStatus.OK);
		ClaimDto singleClaim = getSingleResponse.getBody();
		assertEquals(singleClaim, actual.get(0));

		ResponseEntity<?> getSingleResponse2 = restTemplate.exchange(
				getRootUrl() + "/claims/operator/" + -2,
				HttpMethod.GET, entity, Void.class
		);
		assertNotNull(getSingleResponse2);
		assertEquals(HttpStatus.BAD_REQUEST, getSingleResponse2.getStatusCode());

		getSingleResponse2 = restTemplate.exchange(
				getRootUrl() + "/claims/operator/" + 99999999,
				HttpMethod.GET, entity, Void.class
		);
		assertNotNull(getSingleResponse2);
		assertEquals(HttpStatus.NOT_FOUND, getSingleResponse2.getStatusCode());
	}

	@Test
	public void getAdminClaimsErrors() {

		final String jwt = authUserGetJwt(ADMIN_NAME, ADMIN_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);
		final HttpEntity<UpdateRolesRequest> entity = new HttpEntity<>(headers);

		// Wrong sort
		ResponseEntity<List<ClaimDto>> getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/admin?sort=X",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());

		// Wrong from
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/admin?from=-1",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatusCode());
	}

	@Test
	public void getAdminClaims() {

		if (claimsRepository.findAll().size() <= MAX_TEST_CLAIMS_COUNT/ 4) {
			createClaimsForTests();
		}

		final String jwt = authUserGetJwt(ADMIN_NAME, ADMIN_PASSWORD);
		final HttpHeaders headers = getHeaderWithJwt(jwt);
		final HttpEntity entity = new HttpEntity<>(headers);

		// Check SORT=DESC, FROM=0
		ResponseEntity<List<ClaimDto>> getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/admin",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		List<ClaimDto> result = getResponse.getBody();
		List<ClaimDto> actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT)
						|| entry.getStatus().equals(ERequestStatus.REJECTED)
						|| entry.getStatus().equals(ERequestStatus.RECEIVED))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt).reversed())
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));

		// Check SORT=DESC, FROM=2
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/admin?from=2",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		result = getResponse.getBody();
		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT)
						|| entry.getStatus().equals(ERequestStatus.REJECTED)
						|| entry.getStatus().equals(ERequestStatus.RECEIVED))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt).reversed())
				.skip(2*5)
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));

		// Check SORT=ASC, FROM=0
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/admin?sort=ASC",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		result = getResponse.getBody();
		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT)
						|| entry.getStatus().equals(ERequestStatus.REJECTED)
						|| entry.getStatus().equals(ERequestStatus.RECEIVED))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt))
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));

		// Check SORT=ASC, FROM=1
		getResponse = restTemplate.exchange(
				getRootUrl() + "/claims/admin?sort=ASC&from=1",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<>() {}
		);
		assertNotNull(getResponse);
		assertEquals(getResponse.getStatusCode(), HttpStatus.OK);

		result = getResponse.getBody();
		actual = claimsRepository.findAll().stream()
				.filter(entry -> entry.getStatus().equals(ERequestStatus.SENT)
						|| entry.getStatus().equals(ERequestStatus.REJECTED)
						|| entry.getStatus().equals(ERequestStatus.RECEIVED))
				.sorted(Comparator.comparing(ClaimEntry::getCreatedAt))
				.skip(1*5)
				.limit(5)
				.map(claimService::convertToDto)
				.collect(Collectors.toList());

		assertTrue(result.size() > 0);
		assertEquals(actual.size(), result.size());
		assertTrue(result.equals(actual));
	}

	@Test
	@Disabled
	void createUsers() {
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		final UserEntry user = new UserEntry(USER_NAME, Set.of(ERole.USER), passwordEncoder.encode(USER_PASSWORD));
		usersRepository.save(user);

		final UserEntry operator = new UserEntry(OPERATOR_NAME, Set.of(ERole.USER, ERole.OPERATOR), passwordEncoder.encode(OPERATOR_PASSWORD));
		usersRepository.save(operator);

		final UserEntry admin = new UserEntry(ADMIN_NAME, Set.of(ERole.USER, ERole.ADMIN), passwordEncoder.encode(ADMIN_PASSWORD));
		usersRepository.save(admin);
	}

	@Test
	@Disabled
	void deleteAllUsers() {
		usersRepository.deleteAll();
	}

	private void createClaimsForTests() {

		final UserEntry user = usersRepository.findByName(USER_NAME).orElse(null);
		assertNotNull(user);

		for (int i = 0; i < MAX_TEST_CLAIMS_COUNT; i++) {
			ClaimEntry claimEntry = new ClaimEntry(
					0,
					user,
					ERequestStatus.DRAFT,
					"+7111" + i,
					2,
					2,
					"request: " + UUID.randomUUID(),
					Date.from(Instant.now())
			);
			claimsRepository.save(claimEntry);

			claimEntry = new ClaimEntry(
					0,
					user,
					ERequestStatus.SENT,
					"+72222" + i,
					2,
					2,
					"request: " + UUID.randomUUID(),
					Date.from(Instant.now())
			);
			claimsRepository.save(claimEntry);

			claimEntry = new ClaimEntry(
					0,
					user,
					ERequestStatus.RECEIVED,
					"+73333" + i,
					2,
					2,
					"request: " + UUID.randomUUID(),
					Date.from(Instant.now())
			);
			claimsRepository.save(claimEntry);

			claimEntry = new ClaimEntry(
					0,
					user,
					ERequestStatus.REJECTED,
					"+74444" + i,
					2,
					2,
					"request: " + UUID.randomUUID(),
					Date.from(Instant.now())
			);
			claimsRepository.save(claimEntry);
		}
	}


	private String authUserGetJwt(String username, String password) {
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

	private void logoutByJwt(String jwt) {
		final HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

		HttpEntity<UpdateRolesRequest> entity = new HttpEntity<UpdateRolesRequest>(headers);
		ResponseEntity<Void> response = restTemplate.exchange(
				getRootUrl() + "/auth/logout", HttpMethod.PUT, entity, Void.class
		);

		assertNotNull(response);
		assertEquals(response.getStatusCode(), HttpStatus.OK);
	}

	private HttpHeaders getHeaderWithJwt(String jwt) {
		final HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		return headers;
	}
}
