package com.ghanaride.repository;

import com.ghanaride.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, String> {
    List<Coupon> findByActiveTrueOrderByCreatedAtDesc();
}
