package com.echommo.repository;

import com.echommo.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Integer> {

    // 1. Tìm tất cả đồ của user, sắp xếp đồ đang mặc lên trước
    List<UserItem> findByUser_UserIdOrderByIsEquippedDesc(Integer userId);

    // [MỚI] 2. Tìm UserItem đang được mặc (IsEquipped=True) của một User cho một Loại trang bị (Item.type)
    // Giả định UserItem entity có liên kết @ManyToOne Item item.
    UserItem findByUser_UserIdAndItem_TypeAndIsEquippedTrue(Integer userId, String itemType);
}