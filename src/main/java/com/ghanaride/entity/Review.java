package com.ghanaride.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Review entity for driver/trip ratings.
 */
@Entity
@Table(
    name = "reviews",
    indexes = {
        @Index(name = "idx_review_booking", columnList = "booking_id", unique = true),
        @Index(name = "idx_review_driver", columnList = "driver_id"),
        @Index(name = "idx_review_user", columnList = "user_id"),
        @Index(name = "idx_review_rating", columnList = "rating"),
        @Index(name = "idx_review_created", columnList = "created_at")
    }
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;

    @Column(name = "is_visible")
    private Boolean isVisible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Review() {
    }

    public Review(Long id, Booking booking, User user, User driver, Integer rating, String comment,
                  Boolean isAnonymous, Boolean isVisible, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.booking = booking;
        this.user = user;
        this.driver = driver;
        this.rating = rating;
        this.comment = comment;
        this.isAnonymous = isAnonymous;
        this.isVisible = isVisible;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Review review = new Review();

        public Builder id(Long id) { review.setId(id); return this; }
        public Builder booking(Booking booking) { review.setBooking(booking); return this; }
        public Builder user(User user) { review.setUser(user); return this; }
        public Builder driver(User driver) { review.setDriver(driver); return this; }
        public Builder rating(Integer rating) { review.setRating(rating); return this; }
        public Builder comment(String comment) { review.setComment(comment); return this; }
        public Builder isAnonymous(Boolean isAnonymous) { review.setIsAnonymous(isAnonymous); return this; }
        public Builder isVisible(Boolean isVisible) { review.setIsVisible(isVisible); return this; }
        public Builder createdAt(LocalDateTime createdAt) { review.setCreatedAt(createdAt); return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { review.setUpdatedAt(updatedAt); return this; }
        public Review build() { return review; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Boolean getIsAnonymous() { return isAnonymous; }
    public void setIsAnonymous(Boolean isAnonymous) { this.isAnonymous = isAnonymous; }
    public Boolean getIsVisible() { return isVisible; }
    public void setIsVisible(Boolean isVisible) { this.isVisible = isVisible; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}