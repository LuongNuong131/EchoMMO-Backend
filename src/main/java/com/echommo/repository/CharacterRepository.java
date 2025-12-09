package com.echommo.repository;

import com.echommo.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Integer> {

    Optional<Character> findByUser_UserId(Integer userId);

    boolean existsByName(String name);

    // [FIX] Sửa tên hàm cho đúng với tên biến trong Entity (level, currentExp)
    // JPA sẽ tự động hiểu câu lệnh này thành: SELECT * ... ORDER BY level DESC, current_exp DESC LIMIT 10
    List<Character> findTop10ByOrderByLevelDescCurrentExpDesc();
}