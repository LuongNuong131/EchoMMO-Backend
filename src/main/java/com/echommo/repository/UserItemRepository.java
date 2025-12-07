package com.echommo.repository;

import com.echommo.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    // Lấy tất cả đồ của user (cho hành trang)
    List<UserItem> findByUser_UserId(Long userId);

    // Lấy tất cả đồ ĐANG MẶC (để tính chỉ số cộng thêm khi đánh nhau)
    List<UserItem> findByUser_UserIdAndIsEquippedTrue(Long userId);

    // Tìm món đồ ĐANG MẶC theo LOẠI (ví dụ: tìm xem đang mặc cái áo nào không)
    // Dùng để: Mặc áo mới -> tìm áo cũ tháo ra
    Optional<UserItem> findByUser_UserIdAndItem_TypeAndIsEquippedTrue(Long userId, String type);
}