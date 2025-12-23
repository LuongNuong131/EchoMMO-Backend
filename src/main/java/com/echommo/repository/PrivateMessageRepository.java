package com.echommo.repository;

import com.echommo.entity.PrivateMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Integer> {

    // Lấy lịch sử chat
    @Query("SELECT m FROM PrivateMessage m WHERE (m.sender.userId = :u1 AND m.receiver.userId = :u2) OR (m.sender.userId = :u2 AND m.receiver.userId = :u1) ORDER BY m.sentAt ASC")
    List<PrivateMessage> findConversation(Integer u1, Integer u2);

    // [MỚI] Lấy tất cả tin nhắn liên quan đến user để lọc ra danh sách hội thoại
    @Query("SELECT m FROM PrivateMessage m WHERE m.sender.userId = :userId OR m.receiver.userId = :userId ORDER BY m.sentAt DESC")
    List<PrivateMessage> findAllByUserId(Integer userId);

    // [MỚI] Đếm tin chưa đọc
    long countByReceiver_UserIdAndIsReadFalse(Integer userId);

    // [MỚI] Đếm tin chưa đọc từ 1 người cụ thể
    long countBySender_UserIdAndReceiver_UserIdAndIsReadFalse(Integer senderId, Integer receiverId);

    // [MỚI] Đánh dấu đã đọc
    @Modifying
    @Query("UPDATE PrivateMessage m SET m.isRead = true WHERE m.sender.userId = :senderId AND m.receiver.userId = :receiverId")
    void markAsRead(Integer senderId, Integer receiverId);
}