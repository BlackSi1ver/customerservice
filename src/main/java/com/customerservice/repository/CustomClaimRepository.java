package com.customerservice.repository;

import com.customerservice.domain.ClaimEntry;
import jakarta.persistence.LockModeType;

public interface CustomClaimRepository {
    ClaimEntry lockById(Long id, LockModeType lockMode);
}
