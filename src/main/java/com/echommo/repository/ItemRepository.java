package com.echommo.repository;

import com.echommo.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {
    // Tìm item theo tên (ví dụ: "Kiếm Gỗ")
    Optional<Item> findByName(String name);

    // Tìm item theo loại (ví dụ: "WEAPON")
    boolean existsByType(String type);
}