package com.echommo.repository;

import com.echommo.entity.MarketListing;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MarketListingRepository extends JpaRepository<MarketListing, Long> {

    // [FIX] listedAt (đã sửa từ trước)
    List<MarketListing> findByStatusOrderByListedAtDesc(String status);

    // [FIX QUAN TRỌNG] Đổi Long userId -> Integer userId
    List<MarketListing> findBySeller_UserIdAndStatus(Integer userId, String status);

    @Modifying
    @Transactional
    @Query("DELETE FROM MarketListing m WHERE m.item.itemId = :itemId")
    void deleteAllByItemId(Integer itemId);
}