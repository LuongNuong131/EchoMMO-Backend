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
}