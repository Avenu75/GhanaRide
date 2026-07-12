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
 * Wallet Transaction Repository - Transaction history queries.
 */
@Repository
@Transactional(readOnly = true)
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByUser(User user);

    Page<WalletTransaction> findByUser(User user, Pageable pageable);

    @Query("SELECT t FROM WalletTransaction t WHERE t.user = :user ORDER BY t.createdAt DESC")
    Page<WalletTransaction> findByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT t FROM WalletTransaction t WHERE t.user = :user ORDER BY t.createdAt DESC")
    List<WalletTransaction> findTop10ByUserOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT t FROM WalletTransaction t WHERE t.user = :user AND t.type = :type ORDER BY t.createdAt DESC")
    List<WalletTransaction> findByUserAndType(@Param("user") User user, @Param("type") WalletTransaction.TxType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.user = :user AND t.type = 'TOPUP' AND t.status = 'SUCCESS'")
    BigDecimal getTotalTopups(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.user = :user AND t.type = 'PAYMENT' AND t.status = 'SUCCESS'")
    BigDecimal getTotalPayments(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.user = :user AND t.type = 'LOYALTY_EARN' AND t.status = 'SUCCESS'")
    BigDecimal getTotalLoyaltyEarned(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.user = :user AND t.type = 'REFUND' AND t.status = 'SUCCESS'")
    BigDecimal getTotalRefunds(@Param("user") User user);

    @Query("SELECT t FROM WalletTransaction t WHERE t.reference = :ref")
    Optional<WalletTransaction> findByReference(@Param("ref") String reference);

    @Query("SELECT t FROM WalletTransaction t WHERE t.provider = :provider AND t.providerRef = :providerRef")
    Optional<WalletTransaction> findByProviderAndProviderRef(@Param("provider") String provider, @Param("providerRef") String providerRef);
}