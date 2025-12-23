package com.echommo.service;

import com.echommo.entity.Friendship;
import com.echommo.entity.User;
import com.echommo.enums.NotificationType; // [FIX] Import Enum
import com.echommo.repository.FriendshipRepository;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FriendshipService {

    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    public String sendRequest(String targetUsername) {
        User me = getCurrentUser();
        if (me.getUsername().equals(targetUsername)) throw new RuntimeException("Không thể tự kết bạn!");

        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("Người chơi không tồn tại"));

        if (friendshipRepository.findExistingFriendship(me.getUserId(), target.getUserId()).isPresent()) {
            throw new RuntimeException("Đã có kết nối!");
        }

        Friendship f = new Friendship();
        f.setRequester(me);
        f.setAddressee(target);
        f.setStatus("PENDING");
        friendshipRepository.save(f);

        // [FIX] Sử dụng NotificationType.INFO thay vì String "INFO"
        notificationService.sendNotification(target, "📩 Lời mời kết bạn",
                me.getUsername() + " muốn kết bạn với bạn.", NotificationType.INFO);

        return "Đã gửi lời mời tới " + targetUsername;
    }

    public String acceptRequest(Integer friendshipId) {
        User me = getCurrentUser();
        Friendship f = friendshipRepository.findById(friendshipId).orElseThrow();

        if (!f.getAddressee().getUserId().equals(me.getUserId())) throw new RuntimeException("Lỗi quyền");

        f.setStatus("ACCEPTED");
        friendshipRepository.save(f);

        // [FIX] Sử dụng NotificationType.SUCCESS thay vì String "SUCCESS"
        notificationService.sendNotification(f.getRequester(), "🤝 Kết bạn thành công",
                me.getUsername() + " đã chấp nhận lời mời của bạn!", NotificationType.SUCCESS);

        return "Đã trở thành bạn bè.";
    }

    public String removeFriend(Integer friendshipId) {
        User me = getCurrentUser();
        Friendship f = friendshipRepository.findById(friendshipId).orElseThrow();
        friendshipRepository.delete(f);
        return "Đã xóa bạn.";
    }

    public List<Friendship> getFriendList() {
        return friendshipRepository.findAllFriends(getCurrentUser().getUserId());
    }

    public List<Friendship> getPendingRequests() {
        return friendshipRepository.findByAddressee_UserIdAndStatus(getCurrentUser().getUserId(), "PENDING");
    }
}