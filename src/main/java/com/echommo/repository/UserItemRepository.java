package com.echommo.repository;

import com.echommo.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Integer> {
    // Lấy tất cả đồ của user (cho hành trang)
    List<UserItem> findByUser_UserId(Integer userId);

    // Lấy tất cả đồ ĐANG MẶC (để tính chỉ số cộng thêm khi đánh nhau)
    List<UserItem> findByUser_UserIdAndIsEquippedTrue(Integer userId);

    // Tìm món đồ ĐANG MẶC theo LOẠI
    Optional<UserItem> findByUser_UserIdAndItem_TypeAndIsEquippedTrue(Integer userId, String type);

    // [FIX] Thêm hàm này để MarketplaceService sử dụng (logic gộp stack)
    List<UserItem> findByUser_UserIdOrderByIsEquippedDesc(Integer userId);
}