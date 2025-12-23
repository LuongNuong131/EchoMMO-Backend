package com.echommo.repository;

import com.echommo.entity.PvpMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PvpMatchRepository extends JpaRepository<PvpMatch, Long> {

    // [THÊM DÒNG NÀY] Để tìm danh sách trận đấu theo trạng thái (ACTIVE, FINISHED...)
    List<PvpMatch> findAllByStatus(String status);

    @Query("SELECT m FROM PvpMatch m WHERE (m.player1.charId = :charId OR m.player2.charId = :charId) AND m.status != 'FINISHED'")
    Optional<PvpMatch> findActiveMatchByCharId(Integer charId);
}