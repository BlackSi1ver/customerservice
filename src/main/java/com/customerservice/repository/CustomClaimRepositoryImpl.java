package com.customerservice.repository;

import com.customerservice.domain.ClaimEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

@Service
public class CustomClaimRepositoryImpl implements CustomClaimRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ClaimEntry lockById(Long id, LockModeType lockMode) {
        return entityManager.find(ClaimEntry.class, id, lockMode);
    }
}
