package com.echommo.repository;

import com.echommo.entity.BattleSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BattleSessionRepository extends JpaRepository<BattleSession, Integer> {
    // Tìm session theo Character ID (Integer)
    List<BattleSession> findByCharacter_CharId(Integer charId);
}