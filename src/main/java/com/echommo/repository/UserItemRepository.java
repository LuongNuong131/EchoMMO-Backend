package com.echommo.repository;

import com.echommo.entity.Character;
import com.echommo.entity.Item;
import com.echommo.entity.UserItem;
import com.echommo.enums.SlotType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    int countByCharacter_CharId(Integer charId);

    List<UserItem> findByCharacter_CharId(Integer charId);
    List<UserItem> findByCharacter_CharIdOrderByAcquiredAtDesc(Integer charId);

    // [ADDED] Hỗ trợ lọc vật phẩm chưa khóa (để hiển thị trong túi đồ có thể bán/xóa)
    List<UserItem> findByCharacter_CharIdAndIsLockedFalseOrderByAcquiredAtDesc(Integer charId);

    List<UserItem> findByCharacter_CharIdAndIsEquippedTrue(Integer charId);

    @Query("SELECT ui FROM UserItem ui WHERE ui.character.charId = :charId AND ui.isEquipped = true AND ui.item.slotType = :slotType")
    Optional<UserItem> findEquippedItemBySlot(@Param("charId") Integer charId, @Param("slotType") SlotType slotType);

    Optional<UserItem> findByCharacter_CharIdAndItem_ItemId(Integer charId, Integer itemId);

    // [ADDED] Tìm item stackable chưa bị khóa để gộp
    Optional<UserItem> findByCharacter_CharIdAndItem_ItemIdAndIsLockedFalse(Integer charId, Integer itemId);

    // [FIXED] Long userItemId
    Optional<UserItem> findByUserItemIdAndCharacter_CharId(Long userItemId, Integer charId);

    Optional<UserItem> findByCharacter_CharIdAndItem_Name(Integer charId, String name);

    Optional<UserItem> findByCharacterAndItem(Character character, Item item);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserItem ui WHERE ui.item.itemId = :itemId")
    void deleteAllByItemId(@Param("itemId") Integer itemId);
}