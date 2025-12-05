package com.echommo.repository;

import com.echommo.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    Wallet findByUser_UserId(Integer userId);

    // Lấy Top 10 người giàu nhất
    List<Wallet> findTop10ByOrderByGoldDesc();
}