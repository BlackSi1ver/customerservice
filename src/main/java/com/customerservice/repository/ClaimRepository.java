package com.customerservice.repository;

import com.customerservice.domain.ClaimEntry;
import com.customerservice.domain.ERequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ClaimRepository extends JpaRepository<ClaimEntry, Long>, CustomClaimRepository {

    List<ClaimEntry> findAllByUserName(String username, Pageable pageable);

    List<ClaimEntry> findAllByStatusAndUserNameLike(ERequestStatus status, String username, Pageable pageable);

    Optional<ClaimEntry> findByIdAndStatus(long id, ERequestStatus status);

    List<ClaimEntry> findAllByStatusIn(Set<ERequestStatus> statuses, Pageable pageable);
}
