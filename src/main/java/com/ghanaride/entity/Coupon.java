package com.ghanaride.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {
    public enum DiscountType { PERCENT, FIXED }

    @Id
    @Column(length = 32)
    private String code; // e.g. WELCOME50

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Builder.Default
    private Integer maxUses = 1000;
    @Builder.Default
    private Integer usedCount = 0;

    @Builder.Default
    private Boolean firstRideOnly = false;
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public boolean isValid() {
        if (!Boolean.TRUE.equals(active)) return false;
        LocalDateTime now = LocalDateTime.now();
        if (startsAt != null && now.isBefore(startsAt)) return false;
        if (expiresAt != null && now.isAfter(expiresAt)) return false;
        if (maxUses != null && usedCount != null && usedCount >= maxUses) return false;
        return true;
    }
}
