package com.echommo.service;

import com.echommo.dto.CaptchaResponse;
import com.echommo.entity.Item;
import com.echommo.entity.User;
import com.echommo.repository.ItemRepository;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {

    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;

    private final Map<String, Integer> captchaAnswers = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // Kiểm tra xem user có đang bị khóa mõm không
    public void checkLockStatus(User user) {
        if (user.getCaptchaLockedUntil() != null) {
            if (user.getCaptchaLockedUntil().isAfter(LocalDateTime.now())) {
                long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), user.getCaptchaLockedUntil());
                throw new RuntimeException("LOCKED_WAIT_" + secondsLeft); // Frontend sẽ parse số giây này
            } else {
                // Hết thời gian phạt -> Cho phép giải captcha lại (nhưng chưa reset failCount)
                // Chỉ khi giải ĐÚNG mới reset failCount
            }
        }
    }

    public CaptchaResponse generateCaptcha() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // Check phạt trước khi cho tạo captcha mới
        checkLockStatus(user);

        List<Item> allItems = itemRepository.findAll();
        Collections.shuffle(allItems);
        List<Item> options = allItems.subList(0, 3);
        Item correctItem = options.get(random.nextInt(3));

        captchaAnswers.put(username, correctItem.getItemId());

        CaptchaResponse res = new CaptchaResponse();
        res.setQuestion("Hãy chọn: " + correctItem.getName());
        res.setOptions(options);
        return res;
    }

    @Transactional
    public String solveCaptcha(Integer selectedItemId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // Check lại lần nữa cho chắc
        checkLockStatus(user);

        if (!captchaAnswers.containsKey(username)) return "Captcha lỗi, hãy tải lại.";

        Integer correctAnswer = captchaAnswers.get(username);

        if (correctAnswer.equals(selectedItemId)) {
            // === ĐÚNG: RESET HẾT ===
            user.setIsCaptchaLocked(false);
            user.setCaptchaFailCount(0);
            user.setCaptchaLockedUntil(null);
            userRepository.save(user);
            captchaAnswers.remove(username);
            return "Chính xác! Mở khóa.";
        } else {
            // === SAI: PHẠT MŨ NHÂN ===
            int currentFail = user.getCaptchaFailCount() == null ? 0 : user.getCaptchaFailCount();
            // Lần 1 (count=0) -> 10s. Lần 2 (count=1) -> 20s. Lần 3 (count=2) -> 40s
            // Công thức: 10 * 2^count
            long secondsToLock = 10L * (long) Math.pow(2, currentFail);

            user.setCaptchaFailCount(currentFail + 1);
            user.setCaptchaLockedUntil(LocalDateTime.now().plusSeconds(secondsToLock));
            userRepository.save(user);

            throw new RuntimeException("Sai rồi! Bạn bị khóa " + secondsToLock + " giây.");
        }
    }
}