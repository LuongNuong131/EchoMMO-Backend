package com.echommo.repository;

import com.echommo.entity.User;
import com.echommo.entity.Wallet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser(User user);

    // [FIX] User ID là Integer
    Optional<Wallet> findByUser_UserId(Integer userId);

    // BXH Tài phú
    @Query("SELECT w FROM Wallet w ORDER BY w.gold DESC")
    List<Wallet> findTopWealth(Pageable pageable);
}