package com.echommo.repository;

import com.echommo.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

    // Tìm item theo tên (để map Gỗ/Đá từ ví vào túi)
    Optional<Item> findByName(String name);

    List<Item> findByType(String type);

    // [MỚI] 1. Lấy toàn bộ Inventory của User
    // Dựa trên liên kết @ManyToOne User user trong Entity Item.
    List<Item> findByUser_UserId(Integer userId);

    // [MỚI] 2. Tìm Item cùng loại đang được mặc bởi User
    // Dùng cho logic gỡ/mặc trang bị: tìm Item liên kết với User, có cùng Type, và IsEquipped = true.
    Item findByUser_UserIdAndTypeAndIsEquippedTrue(Integer userId, String type);
}
