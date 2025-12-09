package com.echommo.repository;

import com.echommo.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    // 1. Lấy tất cả đồ của user (Cơ bản)
    List<UserItem> findByUser_UserId(Integer userId);

    // 👇 [FIX] Hàm bị thiếu gây lỗi ở MarketplaceService (Lấy đồ user, sắp xếp đồ đang mặc lên trước)
    List<UserItem> findByUser_UserIdOrderByIsEquippedDesc(Integer userId);

    // 2. Tìm đồ đang mặc theo loại (để tháo ra khi mặc đồ mới)
    Optional<UserItem> findByUser_UserIdAndItem_TypeAndIsEquippedTrue(Integer userId, String type);

    // 3. Lấy danh sách đồ đang mặc (để tính chỉ số nhân vật)
    List<UserItem> findByUser_UserIdAndIsEquippedTrue(Integer userId);

    // 4. Tìm item theo ID vật phẩm (để nhặt đồ, stack số lượng)
    Optional<UserItem> findByUser_UserIdAndItem_ItemId(Integer userId, Integer itemId);

    // 5. Tìm item theo tên (để tìm nguyên liệu cường hóa: Gỗ, Sắt...)
    Optional<UserItem> findByUser_UserIdAndItem_Name(Integer userId, String name);
}