package com.echommo.repository;

import com.echommo.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Integer> {

    // Tìm mối quan hệ giữa 2 người (để check xem đã add chưa)
    @Query("SELECT f FROM Friendship f WHERE (f.requester.userId = :u1 AND f.addressee.userId = :u2) OR (f.requester.userId = :u2 AND f.addressee.userId = :u1)")
    Optional<Friendship> findExistingFriendship(Integer u1, Integer u2);

    // Lấy danh sách lời mời đã nhận (Status = PENDING, Addressee = Me)
    List<Friendship> findByAddressee_UserIdAndStatus(Integer userId, String status);

    // Lấy danh sách bạn bè (Status = ACCEPTED, Requester hoặc Addressee là Me)
    @Query("SELECT f FROM Friendship f WHERE f.status = 'ACCEPTED' AND (f.requester.userId = :userId OR f.addressee.userId = :userId)")
    List<Friendship> findAllFriends(Integer userId);
}