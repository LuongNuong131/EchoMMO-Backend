package com.echommo.repository;

import com.echommo.entity.GiftCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GiftCodeUsageRepository extends JpaRepository<GiftCodeUsage, Long> {
    // Kiểm tra xem user này đã nhập code này chưa
    boolean existsByUserUserIdAndGiftCodeCode(Integer userId, String code);
}