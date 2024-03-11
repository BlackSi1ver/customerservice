package com.example.customerservice.repositories;

import com.example.customerservice.data.ClaimEntry;
import com.example.customerservice.data.ERequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ClaimsRepository extends JpaRepository<ClaimEntry, Long> {
    List<ClaimEntry> findAllByUserName(String username, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE ClaimEntry u " +
            "SET u.phone = :phone, u.request = :request " +
            "WHERE u.id = :id")
    void updateClaimById(long id, String phone, String request);

    @Modifying
    @Transactional
    @Query("UPDATE ClaimEntry u " +
            "SET u.status = :status " +
            "WHERE u.id = :id ")
    void updateClaimStatusById(long id, ERequestStatus status);

    List<ClaimEntry> findAllByStatusAndUserNameLike(ERequestStatus status, String username, Pageable pageable);

    Optional<ClaimEntry> findByIdAndStatus(long id, ERequestStatus status);

    List<ClaimEntry> findAllByStatusIn(Set<ERequestStatus> statuses, Pageable pageable);
}
