package com.echommo.repository;

import com.echommo.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;      // [QUAN TRỌNG] Nhớ import List
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    // Tìm ví theo User ID
    Optional<Wallet> findByUser_UserId(Integer userId);

    // [FIX] Thêm hàm này để LeaderboardService hết lỗi
    // Ý nghĩa: Lấy Top 10 ví có lượng Gold cao nhất giảm dần
    List<Wallet> findTop10ByOrderByGoldDesc();
}