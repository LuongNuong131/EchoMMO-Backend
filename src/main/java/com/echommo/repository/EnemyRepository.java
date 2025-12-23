package com.echommo.repository;

import com.echommo.entity.Enemy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnemyRepository extends JpaRepository<Enemy, Integer> {
    // Tìm quái theo tên (dùng cho map logic)
    Optional<Enemy> findByName(String name);
}