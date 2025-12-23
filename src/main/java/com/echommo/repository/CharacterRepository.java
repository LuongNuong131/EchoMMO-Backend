package com.echommo.repository;

import com.echommo.entity.Character;
import com.echommo.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Integer> {

    Optional<Character> findByUser(User user);

    // [FIX] JPA chuẩn: findBy + User + _ + UserId
    Optional<Character> findByUser_UserId(Integer userId);

    // [FIX] Wrapper để tránh lỗi "No property id found" của JPA khi parse tên hàm
    default Optional<Character> findByUserId(Integer userId) {
        return findByUser_UserId(userId);
    }

    // [FIX] KHÔI PHỤC: Method này để ExplorationService fetch luôn User + Wallet
    // Phải dùng @Param("userId") cho chắc chắn
    @Query("SELECT c FROM Character c JOIN FETCH c.user u JOIN FETCH u.wallet WHERE u.userId = :userId")
    Optional<Character> findByUser_UserIdWithUserAndWallet(@Param("userId") Integer userId);

    boolean existsByName(String name);

    @Query("SELECT c FROM Character c ORDER BY c.level DESC, c.currentExp DESC")
    List<Character> findTopLevels(Pageable pageable);

    @Query("SELECT c FROM Character c ORDER BY c.monsterKills DESC")
    List<Character> findTopMonsterKills(Pageable pageable);
    List<Character> findTop10ByOrderByPvpWinsDesc();
}