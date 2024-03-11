package com.example.customerservice.rest;

import com.example.customerservice.data.*;
import com.example.customerservice.repositories.ClaimsRepository;
import com.example.customerservice.repositories.UsersRepository;
import com.example.customerservice.requests.ChangeClaimStatusRequest;
import com.example.customerservice.requests.CreateClaimRequest;
import com.example.customerservice.requests.UpdateClaimRequest;
import com.example.customerservice.servicies.ClaimService;
import com.example.customerservice.servicies.PhoneDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimsController {

    public final int PAGE_SIZE = 5;

    @Autowired
    private ClaimsRepository claimsRepository;

    @Autowired
    private ClaimService claimService;

    @Autowired
    private PhoneDetailsService phoneDetailsService;

    @Autowired
    private UsersRepository usersRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @ResponseBody
    public ResponseEntity<List<ClaimDto>> getAllUserClaims(
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "from", required = false) Integer from,
            Principal principal) {

        if (from != null && from < 0) {
            throw new IllegalArgumentException("Invalid page value");
        }

        if (sort != null
                && !sort.equalsIgnoreCase(Sort.Direction.DESC.toString())
                && !sort.equalsIgnoreCase(Sort.Direction.ASC.toString())) {

            throw new IllegalArgumentException("Invalid sort value");
        }

        final Pageable pageable = PageRequest.of(
                (from != null) ? from : 0,
                PAGE_SIZE,
                Sort.by(sort != null ? Sort.Direction.fromString(sort) : Sort.Direction.DESC, "createdAt")
        );

        return ResponseEntity.ok(
                claimsRepository.findAllByUserName(principal.getName(), pageable).stream()
                        .map(claimService::convertToDto)
                        .collect(Collectors.toList())
        );
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public void createUserClaim(@RequestBody CreateClaimRequest createRequest,
                                Principal principal) {

        if (createRequest.getPhone() == null || createRequest.getPhone().isEmpty()) {
            throw new IllegalArgumentException("Invalid phone");
        }

        //TODO Better to do it in separate thread
        final PhoneDetails phoneDetails = phoneDetailsService.getPhoneDetails(createRequest.getPhone());

        final UserEntry user = usersRepository.findByName(principal.getName()).orElse(null);
        if (user == null) {
            throw new NoSuchElementException();
        }

        final ClaimEntry claimEntry = new ClaimEntry(
                0,
                user,
                ERequestStatus.DRAFT,
                createRequest.getPhone(),
                phoneDetails.getCountry(),
                phoneDetails.getCity(),
                createRequest.getRequest(),
                Date.from(Instant.now())
        );

        claimsRepository.save(claimEntry);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public void updateUserClaim(@RequestBody UpdateClaimRequest updateRequest,
                                Principal principal) throws IllegalAccessException {

        if (updateRequest.getPhone() == null || updateRequest.getPhone().isEmpty()) {
            throw new IllegalArgumentException("Invalid phone");
        }

        Optional<ClaimEntry> dbEntry = claimsRepository.findById(updateRequest.getId());
        if (dbEntry.isEmpty()) {
            throw new IllegalArgumentException("Invalid id");
        }

        if (!dbEntry.get().getStatus().equals(ERequestStatus.DRAFT)) {
            throw new IllegalArgumentException("Invalid status");
        }

        if (!dbEntry.get().getUser().getName().equals(principal.getName())) {
            throw new IllegalAccessException("Forbidden access to the claim");
        }

        claimsRepository.updateClaimById(
                updateRequest.getId(),
                updateRequest.getPhone(),
                updateRequest.getRequest()
        );
    }

    @PutMapping("/status")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_OPERATOR')")
    public void changeClaimStatus(@RequestBody ChangeClaimStatusRequest updateRequest,
                                  Principal principal,
                                  Authentication authentication) {

        final Optional<ClaimEntry> dbEntry = claimsRepository.findById(updateRequest.getId());
        if (dbEntry.isEmpty()) {
            throw new IllegalArgumentException("Invalid id");
        }

        final Collection<?> authorities = authentication.getAuthorities();

        if (authorities.contains(new SimpleGrantedAuthority("ROLE_OPERATOR"))) {

            if (!(
                    // Existing status
                    dbEntry.get().getStatus().equals(ERequestStatus.SENT)
                            // New status
                            && (
                                    updateRequest.getStatus().equals(ERequestStatus.RECEIVED)
                                            || updateRequest.getStatus().equals(ERequestStatus.REJECTED)
                               )
                 )
            ) {
                throw new IllegalArgumentException("Illegal status change");
            }

        } else if (authorities.contains(new SimpleGrantedAuthority("ROLE_USER"))) {

            if (!(
                    // Existing status
                    dbEntry.get().getStatus().equals(ERequestStatus.DRAFT)
                            // New status
                            && updateRequest.getStatus().equals(ERequestStatus.SENT)
                 )
            ) {
                throw new IllegalArgumentException("Illegal status change");
            }

        } else {
            throw new IllegalArgumentException("Unknown status value");
        }

        claimsRepository.updateClaimStatusById(
                updateRequest.getId(),
                updateRequest.getStatus()
        );
    }

    @GetMapping("/operator")
    @PreAuthorize("hasAuthority('ROLE_OPERATOR')")
    public ResponseEntity<List<ClaimDto>> getAllClaimsByOperator(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "from", required = false) Integer from) {

        if (from != null && from < 0) {
            throw new IllegalArgumentException("Invalid page value");
        }

        if (sort != null
                && !sort.equalsIgnoreCase(Sort.Direction.DESC.toString())
                && !sort.equalsIgnoreCase(Sort.Direction.ASC.toString())) {
            throw new IllegalArgumentException("Invalid sort value");
        }

        final Pageable pageable = PageRequest.of(
                (from != null) ? from : 0,
                PAGE_SIZE,
                Sort.by(sort != null ? Sort.Direction.fromString(sort) : Sort.Direction.DESC, "createdAt")
        );

        String seekName = (username != null && !username.isEmpty())
                ? "%" + username + "%" : "%";

        return ResponseEntity.ok(
                claimsRepository.findAllByStatusAndUserNameLike(ERequestStatus.SENT, seekName, pageable).stream()
                        .map(claimService::convertToDto)
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/operator/{id}")
    @PreAuthorize("hasAuthority('ROLE_OPERATOR')")
    public ResponseEntity<ClaimDto> getClaimsByOperatorById(@PathVariable Long id) {

        if (id == null || id < 0) {
            throw new IllegalArgumentException("Invalid id value");
        }

        final Optional<ClaimEntry> result = claimsRepository.findByIdAndStatus(id, ERequestStatus.SENT);

        if (result.isEmpty()) {
            throw new NoSuchElementException();
        }

        return ResponseEntity.ok(
                claimService.convertToDto(result.get())
        );
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ClaimDto>> getAllClaimsByAdmin(
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "from", required = false) Integer from) {

        if (from != null && from < 0) {
            throw new IllegalArgumentException("Invalid page value");
        }

        if (sort != null
                && !sort.equalsIgnoreCase(Sort.Direction.DESC.toString())
                && !sort.equalsIgnoreCase(Sort.Direction.ASC.toString())) {
            throw new IllegalArgumentException("Invalid sort value");
        }

        final Pageable pageable = PageRequest.of(
                (from != null) ? from : 0,
                PAGE_SIZE,
                Sort.by(sort != null ? Sort.Direction.fromString(sort) : Sort.Direction.DESC, "createdAt")
        );

        return ResponseEntity.ok(
                claimsRepository.findAllByStatusIn(
                        Set.of(ERequestStatus.SENT, ERequestStatus.RECEIVED, ERequestStatus.REJECTED),
                        pageable
                ).stream()
                        .map(claimService::convertToDto)
                        .collect(Collectors.toList())
        );
    }
}
