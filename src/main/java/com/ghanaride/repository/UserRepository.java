package com.ghanaride.repository;

import com.ghanaride.entity.Role;
import com.ghanaride.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity.
 *
 * Key queries:
 * - findByUsernameOrEmail(): Supports login with
 *   either username or email
 * - searchByNameOrEmail(): Admin user search
 * - countByRoleAndEmailVerified(): Homepage stats
 */
@Repository
public interface UserRepository
        extends JpaRepository<User, Long> {

    // =========================================================
    // FIND SINGLE USER
    // =========================================================

    @Transactional(readOnly = true)
    Optional<User> findByUsername(String username);

    @Transactional(readOnly = true)
    Optional<User> findByEmail(String email);

    /**
     * Find by username OR email.
     * Used for login (supports both formats).
     */
    @Transactional(readOnly = true)
    Optional<User> findByUsernameOrEmail(
            String username, String email
    );

    // =========================================================
    // FIND MULTIPLE USERS
    // =========================================================

    @Transactional(readOnly = true)
    List<User> findByRole(Role role);

    @Transactional(readOnly = true)
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Admin user search by name, email, or username.
     * Case-insensitive partial match.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.fullName)
              LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(u.email)
              LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(u.username)
              LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY u.createdAt DESC
        """)
    Page<User> searchByNameOrEmail(
            @Param("query") String query,
            Pageable pageable
    );

    /**
     * Find all users with a specific role
     * and paginate.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT u FROM User u
        WHERE u.role = :role
        ORDER BY u.createdAt DESC
        """)
    Page<User> findByRoleOrderByCreatedAtDesc(
            @Param("role") Role role,
            Pageable pageable
    );

    // =========================================================
    // EXISTENCE CHECKS
    // =========================================================

    @Transactional(readOnly = true)
    boolean existsByUsername(String username);

    @Transactional(readOnly = true)
    boolean existsByEmail(String email);

    // =========================================================
    // COUNTS
    // =========================================================

    @Transactional(readOnly = true)
    long countByRole(Role role);

    /**
     * Count verified users by role.
     * Used for homepage stats
     * (e.g., "200+ verified drivers").
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(u) FROM User u
        WHERE u.role = :role
        AND u.emailVerified = :emailVerified
        AND u.enabled = true
        """)
    long countByRoleAndEmailVerified(
            @Param("role") Role role,
            @Param("emailVerified") boolean emailVerified
    );

    /**
     * Count active (enabled) users.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(u) FROM User u
        WHERE u.enabled = true
        """)
    long countActiveUsers();

    /**
     * Count users registered in last N days.
     * For admin growth metrics.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(u) FROM User u
        WHERE u.createdAt >= :since
        """)
    long countNewUsersSince(
            @Param("since") java.time.LocalDateTime since
    );
}