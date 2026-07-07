package com.ghanaride.repository;

import com.ghanaride.entity.Company;
import com.ghanaride.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Company entity.
 */
@Repository
public interface CompanyRepository
        extends JpaRepository<Company, Long> {

    // =========================================================
    // FIND
    // =========================================================

    @Transactional(readOnly = true)
    Optional<Company> findByUser(User user);

    @Transactional(readOnly = true)
    Optional<Company> findByEmail(String email);

    @Transactional(readOnly = true)
    Optional<Company> findByRegistrationNumber(
            String registrationNumber
    );

    // =========================================================
    // SEARCH
    // =========================================================

    /**
     * Search companies by name.
     * For admin company management.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT c FROM Company c
        WHERE LOWER(c.companyName)
              LIKE LOWER(CONCAT('%', :name, '%'))
        ORDER BY c.companyName ASC
        """)
    List<Company> searchByName(
            @Param("name") String name
    );

    /**
     * Find all verified companies.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT c FROM Company c
        WHERE c.verified = true
        ORDER BY c.companyName ASC
        """)
    List<Company> findAllVerified();

    // =========================================================
    // CHECKS
    // =========================================================

    @Transactional(readOnly = true)
    boolean existsByEmail(String email);

    @Transactional(readOnly = true)
    boolean existsByRegistrationNumber(
            String registrationNumber
    );

    // =========================================================
    // COUNTS
    // =========================================================

    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(c) FROM Company c
        WHERE c.verified = true
        """)
    long countVerified();
}