package com.echommo.repository;

import com.echommo.entity.Item;
import com.echommo.enums.Rarity;
import com.echommo.enums.SlotType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

    Optional<Item> findByName(String name);

    List<Item> findByType(String type);

    List<Item> findBySlotType(SlotType slotType);

    List<Item> findByRarity(Rarity rarity);

    // [FIX] Đã XÓA hàm deleteByUser vì Item không có trường User.
    // Item là dữ liệu hệ thống, không được xóa theo User.
}