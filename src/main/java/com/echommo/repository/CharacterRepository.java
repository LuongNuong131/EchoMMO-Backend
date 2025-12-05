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

    // Lấy Top 10 người cấp cao nhất, nếu cùng cấp thì so Exp
    List<Character> findTop10ByOrderByLevelDescCurrentExpDesc();
}