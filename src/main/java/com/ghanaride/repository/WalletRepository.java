package com.ghanaride.repository;

import com.ghanaride.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Wallet Repository - Wallet queries.
 */
@Repository
@Transactional(readOnly = true)
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    Optional<Wallet> findByUser(User user);

    @Query("SELECT w FROM Wallet w LEFT JOIN FETCH w.user WHERE w.user.id = :userId")
    Optional<Wallet> findByUserIdWithUser(@Param("userId") Long userId);

    @Query("SELECT w FROM Wallet w WHERE w.balance > :threshold")
    List<Wallet> findWithBalanceAbove(@Param("threshold") BigDecimal threshold);
}