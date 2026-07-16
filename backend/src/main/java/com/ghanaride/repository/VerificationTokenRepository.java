package com.ghanaride.repository;

import com.ghanaride.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for VerificationToken entity.
 * Used for email verification flow.
 */
@Repository
public interface VerificationTokenRepository
        extends JpaRepository<VerificationToken, Long> {

    // =========================================================
    // FIND
    // =========================================================

    @Transactional(readOnly = true)
    Optional<VerificationToken> findByToken(String token);

    @Transactional(readOnly = true)
    Optional<VerificationToken> findByUserId(Long userId);

    // =========================================================
    // DELETE
    // =========================================================

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);

    /**
     * Delete expired verification tokens.
     * Called by scheduled cleanup.
     */
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM VerificationToken v
        WHERE v.expiresAt < :now
        AND v.verified = false
        """)
    int deleteExpiredTokens(
            @Param("now") LocalDateTime now
    );

    // =========================================================
    // CHECKS
    // =========================================================

    /**
     * Check if a valid token exists for user.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(v) > 0 FROM VerificationToken v
        WHERE v.user.id = :userId
        AND v.verified = false
        AND v.expiresAt > CURRENT_TIMESTAMP
        """)
    boolean hasValidToken(@Param("userId") Long userId);
}