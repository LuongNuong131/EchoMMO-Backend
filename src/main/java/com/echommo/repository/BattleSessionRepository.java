package com.echommo.repository;

import com.echommo.entity.BattleSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BattleSessionRepository extends JpaRepository<BattleSession, Long> {
    Optional<BattleSession> findByUser_UserId(Integer userId);
}