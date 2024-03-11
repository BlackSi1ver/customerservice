package com.example.customerservice.repositories;

import com.example.customerservice.data.ERole;
import com.example.customerservice.data.UserEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Repository
public interface UsersRepository extends JpaRepository<UserEntry, Long> {
    Optional<UserEntry> findByName(String name);

    @Modifying
    @Transactional
    @Query("update UserEntry u set u.roles = :role where u.name = :name")
    void updateUserRoles(@Param(value = "name") String name, @Param(value = "role") Set<ERole> role);
}
