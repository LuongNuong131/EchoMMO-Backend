package com.echommo.repository;

import com.echommo.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Integer> {
    Optional<Character> findByUser_UserId(Integer userId);
    Boolean existsByName(String name);

    // [FIX] Lấy Top 10 người cấp cao nhất, ưu tiên Lv giảm dần, sau đó Exp giảm dần.
    List<Character> findTop10ByOrderByLvDescExpDesc();
}