package com.echommo.repository;

import com.echommo.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // Lấy 20 tin nhắn mới nhất (sắp xếp giảm dần theo thời gian)
    List<Message> findTop20ByOrderBySentAtDesc();
}