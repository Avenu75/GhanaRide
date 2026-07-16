package com.ghanaride.repository;

import com.ghanaride.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByDriverId(Long driverId);

    List<Review> findByPassengerId(Long passengerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.driver.id = :driverId")
    Double getAverageRatingForDriver(@Param("driverId") Long driverId);
}
