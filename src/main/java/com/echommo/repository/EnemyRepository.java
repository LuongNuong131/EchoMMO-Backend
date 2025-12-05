package com.echommo.repository;

import com.echommo.entity.Enemy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EnemyRepository extends JpaRepository<Enemy, Integer> {
    // Tìm quái có level xấp xỉ nhân vật (trong khoảng level - 2 đến level + 2)
    @Query("SELECT e FROM Enemy e WHERE e.level BETWEEN :minLevel AND :maxLevel")
    List<Enemy> findByLevelRange(int minLevel, int maxLevel);
}