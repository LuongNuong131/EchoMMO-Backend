package com.echommo.repository;

import com.echommo.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Integer> {
    // Lấy tất cả skill (Sau này có thể lọc theo class nhân vật)
    List<Skill> findAll();
}