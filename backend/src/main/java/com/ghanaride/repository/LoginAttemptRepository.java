package com.ghanaride.repository;

import com.ghanaride.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for LoginAttempt entity.
 */
@Repository
public interface LoginAttemptRepository
        extends JpaRepository<LoginAttempt, Long> {

    // =========================================================
    // FIND
    // =========================================================

    @Transactional(readOnly = true)
    Optional<LoginAttempt> findByUsername(
            String username
    );

    // =========================================================
    // DELETE
    // =========================================================

    @Modifying
    @Transactional
    void deleteByUsername(String username);

    /**
     * Delete old attempt records.
     * Called by scheduled cleanup (daily at 2am).
     * Returns number of deleted records.
     *
     * FIX: Added this method which was missing
     * but referenced in LoginAttemptService.
     */
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM LoginAttempt a
        WHERE a.lastAttempt < :cutoff
        """)
    int deleteByLastAttemptBefore(
            @Param("cutoff") LocalDateTime cutoff
    );

    // =========================================================
    // CHECKS
    // =========================================================

    /**
     * Check if an account is currently locked.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(a) > 0 FROM LoginAttempt a
        WHERE a.username = :username
        AND a.lockedUntil > CURRENT_TIMESTAMP
        """)
    boolean isAccountLocked(
            @Param("username") String username
    );

    /**
     * Count total locked accounts.
     * For admin security dashboard.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(a) FROM LoginAttempt a
        WHERE a.lockedUntil > CURRENT_TIMESTAMP
        """)
    long countCurrentlyLockedAccounts();
}