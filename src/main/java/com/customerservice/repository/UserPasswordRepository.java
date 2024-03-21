package com.customerservice.repository;

import com.customerservice.domain.UserPasswordEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPasswordRepository extends JpaRepository<UserPasswordEntry, Long> {
    UserPasswordEntry findByUserId(long userId);
}
