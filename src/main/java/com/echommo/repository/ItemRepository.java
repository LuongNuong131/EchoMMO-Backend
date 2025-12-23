package com.echommo.repository;

import com.echommo.entity.Item;
import com.echommo.enums.Rarity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {
    Optional<Item> findByName(String name);
    Optional<Item> findByCode(String code);

    // [FIX] Thêm method này để ShopController gọi được danh sách hàng vô hạn
    List<Item> findByIsSystemItemTrue();

    List<Item> findByRarity(Rarity rarity);
    List<Item> findByRarityAndCodeNotLike(Rarity rarity, String codePattern);
}