package com.echommo.repository;

import com.echommo.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    // Lấy thông báo của user, mới nhất lên đầu
    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    // Đếm số thông báo chưa đọc
    long countByUser_UserIdAndIsReadFalse(Integer userId);
}