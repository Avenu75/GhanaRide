package com.ghanaride.repository;

import com.ghanaride.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Repository - Core user queries.
 */
@Repository
@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.enabled = true")
    List<User> findByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.accountType = :accountType AND u.enabled = true")
    List<User> findByRoleAndAccountType(@Param("role") Role role, @Param("accountType") String accountType);

    @Query("SELECT u FROM User u WHERE u.role IN :roles AND u.enabled = true")
    List<User> findByRoleIn(List<Role> roles);

    List<User> findByRoleAndEnabledTrue(Role role);

    @Query("SELECT u FROM User u WHERE u.phoneNumber IS NOT NULL AND u.phoneNumber != ''")
    List<User> findUsersWithPhone();

    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :since")
    List<User> findActiveSince(@Param("since") LocalDateTime since);

    @Query("SELECT u FROM User u WHERE u.enabled = true")
    List<User> findActiveUsers();

    // =========================================================
    // DRIVER SPECIFIC
    // =========================================================

    @Query("SELECT u FROM User u WHERE u.role = 'DRIVER' AND u.enabled = true")
    List<User> findActiveDrivers();

    @Query("SELECT u FROM User u WHERE u.role = 'DRIVER' AND u.enabled = true AND u.phoneNumber IS NOT NULL AND u.phoneNumber != ''")
    List<User> findVerifiedDrivers();

    // =========================================================
    // COMPANY SPECIFIC
    // =========================================================

    @Query("SELECT u FROM User u WHERE u.role = 'COMPANY' AND u.enabled = true")
    List<User> findActiveCompanies();

    // =========================================================
    // ADMIN QUERIES
    // =========================================================

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    Page<User> findAllOrderByCreatedDesc(Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") Role role);
}