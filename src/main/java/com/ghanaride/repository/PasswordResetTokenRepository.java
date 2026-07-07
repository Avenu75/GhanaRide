package com.ghanaride.repository;

import com.ghanaride.entity.PasswordResetToken;
import com.ghanaride.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for PasswordResetToken entity.
 */
@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    // =========================================================
    // FIND TOKEN
    // =========================================================

    @Transactional(readOnly = true)
    Optional<PasswordResetToken> findByToken(String token);

    @Transactional(readOnly = true)
    Optional<PasswordResetToken> findByUser(User user);

    /**
     * Find most recent token for rate limiting.
     * Used by PasswordResetService to enforce
     * 1-request-per-minute rule.
     *
     * FIX: Added this method which was missing
     * but referenced in PasswordResetService.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT t FROM PasswordResetToken t
        WHERE t.user.email = :identifier
        OR t.user.username = :identifier
        ORDER BY t.createdAt DESC
        """)
    Optional<PasswordResetToken>
    findLatestByEmailOrUsername(
            @Param("identifier") String identifier
    );

    // =========================================================
    // DELETE OPERATIONS
    // =========================================================

    /**
     * Delete all tokens for a user.
     * Called before creating a new token
     * (prevent multiple active tokens).
     */
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM PasswordResetToken t
        WHERE t.user = :user
        """)
    void deleteByUser(@Param("user") User user);

    /**
     * Delete expired tokens.
     * Called by scheduled cleanup job.
     * Returns number of deleted records.
     *
     * FIX: Added this method which was missing
     * but referenced in PasswordResetService.
     */
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM PasswordResetToken t
        WHERE t.expiryDate < :now
        """)
    int deleteByExpiryDateBefore(
            @Param("now") LocalDateTime now
    );

    // =========================================================
    // CHECKS
    // =========================================================

    /**
     * Check if a valid (non-expired) token exists
     * for a user.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(t) > 0 FROM PasswordResetToken t
        WHERE t.user = :user
        AND t.expiryDate > CURRENT_TIMESTAMP
        """)
    boolean existsValidTokenForUser(
            @Param("user") User user
    );
}