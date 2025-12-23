package com.echommo.repository;

import com.echommo.entity.PvpChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PvpChatRepository extends JpaRepository<PvpChat, Long> {
    // Hàm này giúp tìm tin nhắn theo Match ID (nếu cần truy vấn riêng lẻ)
    List<PvpChat> findByMatch_MatchIdOrderByTimestampAsc(Long matchId);
}