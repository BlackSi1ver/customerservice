package com.customerservice.service;

import com.customerservice.domain.*;
import com.customerservice.exception.ForbiddenAccessException;
import com.customerservice.exception.NotFoundClaimException;
import com.customerservice.exception.NotFoundUserException;
import com.customerservice.repository.ClaimRepository;
import com.customerservice.repository.UserRepository;
import com.customerservice.rest.domain.ClaimDto;
import jakarta.persistence.LockModeType;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClaimService {

    public final int PAGE_SIZE = 5;

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PhoneDetailsService phoneDetailsService;
    private final TransactionTemplate transactionTemplate;

    public ClaimService(final UserRepository userRepository,
                        final ClaimRepository claimRepository,
                        final ModelMapper modelMapper,
                        final PhoneDetailsService phoneDetailsService,
                        final TransactionTemplate transactionTemplate) {

        this.userRepository = userRepository;
        this.claimRepository = claimRepository;
        this.modelMapper = modelMapper;
        this.phoneDetailsService = phoneDetailsService;
        this.phoneDetailsService.setExternalDataHandler(new PhoneDetailsPhoneDetailsHandler());
        this.transactionTemplate = transactionTemplate;
    }

    public ClaimDto createClaimWithDetails(final long userId,
                                           final ERequestStatus status,
                                           final PhoneInfo phoneInfo,
                                           final String request,
                                           final Date createdAt) {

        final ClaimDto claim = createClaim(userId, status, phoneInfo, request, createdAt);

        phoneDetailsService.AddPhoneRequest(
                new PhoneDetailsService.PhoneRequest(
                        claim.getId(),
                        claim.getPhone().getPhoneNumber()
                )
        );

        return claim;
    }

    @Transactional
    public ClaimDto createClaim(final long userId,
                                final ERequestStatus status,
                                final PhoneInfo phoneInfo,
                                final String request,
                                final Date createdAt) {

        final UserEntry user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new NotFoundUserException("User not found");
        }

        return convertToDto(
                claimRepository.save(new ClaimEntry(user, status, phoneInfo, request, createdAt))
        );
    }

    @Transactional
    public ClaimDto getClaimById(final long id) {

        return convertToDto(
                claimRepository.findById(id).orElse(null)
        );
    }

    public List<ClaimDto> findAllByName(final String username,
                                        final int from,
                                        final Sort.Direction sort) {

        return claimRepository.findAllByUserName(
                username,
                PageRequest.of(from, PAGE_SIZE, sort, "createdAt"))
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ClaimDto> findAll() {
        return claimRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public long getAllCount() {
        return claimRepository.count();
    }

    public ClaimDto convertToDto(final ClaimEntry claimEntry) {
        if (claimEntry != null) {
            return modelMapper.map(claimEntry, ClaimDto.class);
        }
        return null;
    }

    @Transactional
    public ClaimDto updateClaimStatus(final String username,
                                      final long id,
                                      final List<ERole> roles,
                                      final ERequestStatus status) {

        final ClaimEntry claimEntry = claimRepository.lockById(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (claimEntry == null) {
            throw new NotFoundClaimException("Claim not found");
        }

        checkValid: {

            if (roles.contains(ERole.OPERATOR)) {
                if (claimEntry.getStatus().equals(ERequestStatus.SENT)
                        // New status
                        && (status.equals(ERequestStatus.RECEIVED) || status.equals(ERequestStatus.REJECTED))
                ) {
                    break checkValid;
                }
            } else if (roles.contains(ERole.USER)) {
                if (claimEntry.getUser().getName().equals(username)
                        // Existing status
                        && claimEntry.getStatus().equals(ERequestStatus.DRAFT)
                        // New status
                        && status.equals(ERequestStatus.SENT)
                ) {
                    break checkValid;
                }
            }

            throw new ForbiddenAccessException("Illegal status change");
        }

        claimEntry.setStatus(status);

        return convertToDto(claimRepository.save(claimEntry));
    }


    @Transactional
    public ClaimDto updateClaimById(final String username,
                                    final long id,
                                    final String phone,
                                    final String request) {

        final ClaimEntry claimEntry = claimRepository.lockById(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (claimEntry == null) {
            throw new NotFoundClaimException("Claim not found");
        }

        if (!claimEntry.getStatus().equals(ERequestStatus.DRAFT)) {
            throw new ForbiddenAccessException("Invalid status");
        }

        if (!claimEntry.getUser().getName().equals(username)) {
            throw new ForbiddenAccessException("Operation not allowed");
        }

        claimEntry.setPhone(phone);
        claimEntry.setRequest(request);

        return convertToDto(claimRepository.save(claimEntry));
    }

    public ClaimDto findByIdAndStatus(final Long id,
                                      final ERequestStatus status) {

        final ClaimEntry claimEntry = claimRepository.findByIdAndStatus(id, status).orElse(null);

        if (claimEntry == null) {
            throw new NotFoundClaimException("Claim not found");
        }

        return convertToDto(claimEntry);
    }

    @Transactional
    public void updateClaimPhoneDetailsById(final long id,
                                                final Integer country,
                                                final Integer city) {

        final ClaimEntry claimEntry = claimRepository.lockById(id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (claimEntry == null) {
            throw new NotFoundClaimException("Claim not found");
        }

        claimEntry.setCountry(country);
        claimEntry.setCity(city);

        claimRepository.save(claimEntry);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteClaimById(final long id) {
        claimRepository.deleteById(id);
    }

    public List<ClaimDto> findAllByStatusAndUserNameLike(final ERequestStatus status,
                                                         final String username,
                                                         Pageable pageable) {
        return claimRepository.findAllByStatusAndUserNameLike(status, username, pageable).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ClaimDto> findAllByStatusIn(final Set<ERequestStatus> statuses,
                                            final Pageable pageable) {
        return claimRepository.findAllByStatusIn(statuses, pageable).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private class PhoneDetailsPhoneDetailsHandler implements ExternalPhoneDetailsHandler {

        @Override
        public void saveData(long id, Integer country, Integer city) {
            transactionTemplate.executeWithoutResult(status -> updateClaimPhoneDetailsById(id, country, city));
        }
    }
}
