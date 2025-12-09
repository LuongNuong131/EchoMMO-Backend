package com.echommo.repository;

import com.echommo.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    // 1. Lấy tất cả đồ của user (Dùng User_UserId để JPA hiểu lấy theo field userId của User)
    List<UserItem> findByUser_UserId(Integer userId);

    // 2. Lấy đồ user, sắp xếp đồ đang mặc lên trước (Dùng cho Marketplace/Inventory)
    List<UserItem> findByUser_UserIdOrderByIsEquippedDesc(Integer userId);

    // 3. Tìm đồ đang mặc theo loại (để tháo ra khi mặc đồ mới)
    Optional<UserItem> findByUser_UserIdAndItem_TypeAndIsEquippedTrue(Integer userId, String type);

    // 4. Lấy danh sách tất cả đồ đang mặc (để tính chỉ số nhân vật)
    List<UserItem> findByUser_UserIdAndIsEquippedTrue(Integer userId);

    // 5. Tìm item cụ thể theo ID gốc (để nhặt đồ, stack số lượng)
    Optional<UserItem> findByUser_UserIdAndItem_ItemId(Integer userId, Integer itemId);

    // 6. Tìm item theo tên (để tìm nguyên liệu cường hóa: Gỗ, Sắt...)
    Optional<UserItem> findByUser_UserIdAndItem_Name(Integer userId, String name);
}