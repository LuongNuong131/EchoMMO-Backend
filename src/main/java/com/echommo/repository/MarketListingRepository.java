package com.echommo.repository;

import com.echommo.entity.MarketListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MarketListingRepository extends JpaRepository<MarketListing, Integer> {
    // Lấy danh sách đang bán (ACTIVE), sắp xếp mới nhất
    List<MarketListing> findByStatusOrderByCreatedAtDesc(String status);

    // Lấy danh sách mình đang bán
    List<MarketListing> findBySeller_UserIdAndStatus(Integer userId, String status);
}