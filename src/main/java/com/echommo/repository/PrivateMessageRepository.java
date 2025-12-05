package com.echommo.repository;

import com.echommo.entity.PrivateMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Integer> {

    // Lấy tin nhắn giữa 2 người (chiều đi và chiều về), sắp xếp theo thời gian
    @Query("SELECT m FROM PrivateMessage m WHERE (m.sender.userId = :u1 AND m.receiver.userId = :u2) OR (m.sender.userId = :u2 AND m.receiver.userId = :u1) ORDER BY m.sentAt ASC")
    List<PrivateMessage> findConversation(Integer u1, Integer u2);
}