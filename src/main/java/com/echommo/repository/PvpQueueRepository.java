package com.echommo.repository;

import com.echommo.entity.PvpQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PvpQueueRepository extends JpaRepository<PvpQueue, Integer> {

    Optional<PvpQueue> findByCharId(Integer charId);

    // [FIX] Dùng JPQL để query chính xác theo Entity
    // Điều kiện q.charId <> :charId đảm bảo KHÔNG bao giờ lấy chính người đang query
    @Query("SELECT q FROM PvpQueue q " +
            "WHERE q.charId <> :charId " +
            "AND q.status = 'SEARCHING' " +
            "AND q.level BETWEEN :minLevel AND :maxLevel " +
            "ORDER BY q.joinedAt ASC LIMIT 1")
    Optional<PvpQueue> findMatchCandidate(@Param("charId") Integer charId,
                                          @Param("minLevel") Integer minLevel,
                                          @Param("maxLevel") Integer maxLevel);
}