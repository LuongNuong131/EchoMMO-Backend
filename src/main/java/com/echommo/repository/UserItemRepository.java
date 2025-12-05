package com.echommo.repository;

import com.echommo.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Integer> {
    // Tìm tất cả đồ của user, sắp xếp đồ đang mặc lên trước
    List<UserItem> findByUser_UserIdOrderByIsEquippedDesc(Integer userId);
}