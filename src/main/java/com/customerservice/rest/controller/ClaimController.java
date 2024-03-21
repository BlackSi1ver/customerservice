package com.customerservice.rest.controller;

import com.customerservice.domain.*;
import com.customerservice.exception.InvalidArgumentException;
import com.customerservice.rest.domain.*;
import com.customerservice.service.ClaimService;
import com.customerservice.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/claim")
public class ClaimController {

    public final int PAGE_SIZE = 5;

    private final ClaimService claimService;
    private final UserService userService;

    public ClaimController(final UserService userService,
                           final ClaimService claimService) {

        this.userService = userService;
        this.claimService = claimService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @ResponseBody
    public ResponseEntity<List<ClaimDto>> getAllUserClaims(
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "from", required = false) Integer from,
            Principal principal) {

        if (from != null && from < 0) {
            throw new InvalidArgumentException("Invalid page value");
        }

        if (sort != null
                && !sort.equalsIgnoreCase(Sort.Direction.DESC.toString())
                && !sort.equalsIgnoreCase(Sort.Direction.ASC.toString())) {

            throw new InvalidArgumentException("Invalid sort value");
        }

        return ResponseEntity.ok(
                claimService.findAllByName(
                        principal.getName(),
                        (from != null) ? from : 0,
                        (sort != null) ? Sort.Direction.fromString(sort) : Sort.Direction.DESC)
        );
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<ClaimDto> createClaim(@RequestBody CreateClaimRequest createRequest,
                                                Principal principal) {

        if (createRequest.getPhone() == null || createRequest.getPhone().isEmpty()) {
            throw new InvalidArgumentException("Invalid phone");
        }

        final UserDto user = userService.getUserByName(principal.getName());

        return ResponseEntity.ok(
                claimService.createClaimWithDetails(
                        user.getId(),
                        ERequestStatus.DRAFT,
                        new PhoneInfo(createRequest.getPhone()),
                        createRequest.getRequest(),
                        Date.from(Instant.now())
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<ClaimDto> updateClaim(@PathVariable Long id,
                                                @RequestBody UpdateClaimRequest updateRequest,
                                                Principal principal) {

        if (id == null) {
            throw new InvalidArgumentException("Invalid id");
        }

        if (updateRequest.getPhone() == null || updateRequest.getPhone().isEmpty()) {
            throw new InvalidArgumentException("Invalid phone");
        }

        return ResponseEntity.ok(
                claimService.updateClaimById(
                        principal.getName(),
                        id,
                        updateRequest.getPhone(),
                        updateRequest.getRequest())
        );
    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_OPERATOR')")
    public ResponseEntity<ClaimDto> changeClaimStatus(@PathVariable Long id,
                                                      @RequestBody ChangeClaimStatusRequest updateRequest,
                                                      Principal principal,
                                                      Authentication authentication) {

        if (id == null) {
            throw new InvalidArgumentException("Invalid id");
        }

        final List<ERole> roles = authentication.getAuthorities().stream()
                .map(e -> ERole.valueOf(e.toString().substring("ROLE_".length())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                claimService.updateClaimStatus(principal.getName(), id, roles, updateRequest.getStatus())
        );
    }

    @GetMapping("/operator")
    @PreAuthorize("hasAuthority('ROLE_OPERATOR')")
    public ResponseEntity<List<ClaimDto>> getAllClaimsByOperator(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "from", required = false) Integer from) {

        if (from != null && from < 0) {
            throw new InvalidArgumentException("Invalid page value");
        }

        if (sort != null
                && !sort.equalsIgnoreCase(Sort.Direction.DESC.toString())
                && !sort.equalsIgnoreCase(Sort.Direction.ASC.toString())) {
            throw new InvalidArgumentException("Invalid sort value");
        }

        final Pageable pageable = PageRequest.of(
                (from != null) ? from : 0,
                PAGE_SIZE,
                Sort.by(sort != null ? Sort.Direction.fromString(sort) : Sort.Direction.DESC, "createdAt")
        );

        String seekName = (username != null && !username.isEmpty())
                ? "%" + username + "%" : "%";

        return ResponseEntity.ok(
                claimService.findAllByStatusAndUserNameLike(ERequestStatus.SENT, seekName, pageable)
        );
    }

    @GetMapping("/operator/{id}")
    @PreAuthorize("hasAuthority('ROLE_OPERATOR')")
    public ResponseEntity<ClaimDto> getClaimsByOperatorById(@PathVariable Long id) {

        if (id == null || id < 0) {
            throw new InvalidArgumentException("Invalid id value");
        }

        return ResponseEntity.ok(
                claimService.findByIdAndStatus(id, ERequestStatus.SENT)
        );

    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ClaimDto>> getAllClaimsByAdmin(
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "from", required = false) Integer from) {

        if (from != null && from < 0) {
            throw new InvalidArgumentException("Invalid page value");
        }

        if (sort != null
                && !sort.equalsIgnoreCase(Sort.Direction.DESC.toString())
                && !sort.equalsIgnoreCase(Sort.Direction.ASC.toString())) {
            throw new InvalidArgumentException("Invalid sort value");
        }

        final Pageable pageable = PageRequest.of(
                (from != null) ? from : 0,
                PAGE_SIZE,
                Sort.by(sort != null ? Sort.Direction.fromString(sort) : Sort.Direction.DESC, "createdAt")
        );

        return ResponseEntity.ok(
                claimService.findAllByStatusIn(
                        Set.of(ERequestStatus.SENT, ERequestStatus.RECEIVED, ERequestStatus.REJECTED),
                        pageable
                )
        );
    }
}
