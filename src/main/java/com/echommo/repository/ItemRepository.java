package com.echommo.repository;

import com.echommo.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    // Sửa: Tìm theo User Entity ID (dấu gạch dưới giúp JPA hiểu ta đang chọc vào object User lấy ID)
    List<UserItem> findByUser_Id(Long userId);

    // Tìm item cụ thể của user (ví dụ để kiểm tra sở hữu)
    Optional<UserItem> findByUser_IdAndUserItemId(Long userId, Long userItemId);
}