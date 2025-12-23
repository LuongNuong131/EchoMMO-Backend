package com.echommo.repository;

import com.echommo.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Các hàm cơ bản cũ
    Optional<User> findByUsername(String username);

    // Hàm lấy User kèm theo Wallet (để hiển thị tiền)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.wallet WHERE u.username = :username")
    Optional<User> findByUsernameWithWallet(@Param("username") String username);

    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    // --- CÁC HÀM CHO BẢNG XẾP HẠNG (LEADERBOARD) ---
    // Spring Data JPA sẽ tự động tạo câu query dựa trên tên hàm

    // 1. Top Cao Thủ (Level giảm dần)
    // Truy cập vào entity Character -> field level
    List<User> findTop10ByOrderByCharacter_LevelDesc(Pageable pageable);

    // 2. Top Phú Hộ (Vàng giảm dần)
    // Truy cập vào entity Wallet -> field gold
    List<User> findTop10ByOrderByWallet_GoldDesc(Pageable pageable);

    // 3. Top Trảm Yêu (Số quái giết giảm dần)
    // [FIX LỖI] Truy cập vào entity Character -> field monsterKills (Số nhiều 's')
    List<User> findTop10ByOrderByCharacter_MonsterKillsDesc(Pageable pageable);
}