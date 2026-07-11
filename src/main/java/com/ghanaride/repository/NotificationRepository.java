package com.ghanaride.repository;

import com.ghanaride.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserIdAndReadFlagFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.readFlag=true, n.readAt=CURRENT_TIMESTAMP where n.user.id=?1 and n.readFlag=false")
    int markAllRead(Long userId);
}
