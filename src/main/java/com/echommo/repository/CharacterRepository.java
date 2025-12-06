package com.echommo.repository;

import com.echommo.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Integer> {

    // Tìm nhân vật theo User ID
    Optional<Character> findByUser_UserId(Integer userId);

    // Kiểm tra tên nhân vật tồn tại chưa
    boolean existsByName(String name);

    // [FIX] Thêm hàm này để LeaderboardService không bị lỗi
    // Ý nghĩa: Lấy Top 10, sắp xếp theo Level giảm dần, nếu bằng Level thì xét Exp giảm dần
    List<Character> findTop10ByOrderByLvDescExpDesc();
}