package com.ghanaride.repository;

import com.ghanaride.entity.Car;
import com.ghanaride.entity.Company;
import com.ghanaride.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Car entity.
 */
@Repository
public interface CarRepository
        extends JpaRepository<Car, Long> {

    // =========================================================
    // FIND
    // =========================================================

    @Transactional(readOnly = true)
    List<Car> findByDriver(User driver);

    @Transactional(readOnly = true)
    List<Car> findByDriverId(Long driverId);

    @Transactional(readOnly = true)
    List<Car> findByCompany(Company company);

    @Transactional(readOnly = true)
    Optional<Car> findByPlateNumber(String plateNumber);

    /**
     * Find cars for driver ordered by creation date.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT c FROM Car c
        WHERE c.driver = :driver
        ORDER BY c.createdAt DESC
        """)
    List<Car> findByDriverOrderByCreatedAtDesc(
            @Param("driver") User driver
    );

    // =========================================================
    // EXISTENCE CHECKS
    // =========================================================

    @Transactional(readOnly = true)
    boolean existsByPlateNumber(String plateNumber);

    /**
     * Check if number plate belongs to a specific driver.
     * Used to verify ownership before reusing a plate.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(c) > 0 FROM Car c
        WHERE c.plateNumber = :plate
        AND c.driver = :driver
        """)
    boolean existsByPlateNumberAndDriver(
            @Param("plate") String plate,
            @Param("driver") User driver
    );

    // =========================================================
    // COUNTS
    // =========================================================

    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(c) FROM Car c
        WHERE c.driver = :driver
        """)
    long countByDriver(@Param("driver") User driver);

    @Transactional(readOnly = true)
    @Query("""
        SELECT COUNT(c) FROM Car c
        WHERE c.company = :company
        """)
    long countByCompany(
            @Param("company") Company company
    );

    // =========================================================
    // DELETE
    // =========================================================

    @Modifying
    @Transactional
    void deleteByDriverId(Long driverId);

    @Modifying
    @Transactional
    void deleteByCompanyId(Long companyId);
}