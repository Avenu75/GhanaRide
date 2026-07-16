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

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    // FIXED: Company has List<User> users, not single user
    @Transactional(readOnly = true)
    @Query("SELECT c FROM Company c JOIN c.users u WHERE u = :user")
    Optional<Company> findByUser(@Param("user") User user);

    // Alternative derived query that works with List
    @Transactional(readOnly = true)
    Optional<Company> findByUsersContaining(User user);

    @Transactional(readOnly = true)
    Optional<Company> findByCompanyEmail(String email);

    // Keep compatibility: old code calls findByEmail
    default Optional<Company> findByEmail(String email) {
        return findByCompanyEmail(email);
    }

    @Transactional(readOnly = true)
    Optional<Company> findByRegistrationNumber(String registrationNumber);

    @Transactional(readOnly = true)
    @Query("""
        SELECT c FROM Company c
        WHERE LOWER(c.companyName) LIKE LOWER(CONCAT('%', :name, '%'))
        ORDER BY c.companyName ASC
        """)
    List<Company> searchByName(@Param("name") String name);

    // FIXED: Company has no 'verified' boolean, use status = ACTIVE
    @Transactional(readOnly = true)
    @Query("""
        SELECT c FROM Company c
        WHERE c.status = 'ACTIVE'
        ORDER BY c.companyName ASC
        """)
    List<Company> findAllVerified();

    @Transactional(readOnly = true)
    boolean existsByCompanyEmail(String email);

    default boolean existsByEmail(String email) {
        return existsByCompanyEmail(email);
    }

    @Transactional(readOnly = true)
    boolean existsByRegistrationNumber(String registrationNumber);

    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(c) FROM Company c
        WHERE c.status = 'ACTIVE'
        """)
    long countVerified();

    // Additional safe finder by users id
    @Transactional(readOnly = true)
    @Query("SELECT c FROM Company c JOIN c.users u WHERE u.id = :userId")
    Optional<Company> findByUserId(@Param("userId") Long userId);
}
