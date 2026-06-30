package com.ghanaride.repository;

import com.ghanaride.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    Optional<LoginAttempt> findByUsername(String username);

    @Modifying
    @Transactional
    void deleteByUsername(String username);
}