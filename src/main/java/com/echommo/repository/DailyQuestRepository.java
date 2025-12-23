package com.echommo.repository;

import com.echommo.entity.DailyQuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyQuestRepository extends JpaRepository<DailyQuest, Integer> {
    // [FIX] Hàm này bắt buộc phải có để QuestService gọi
    // Spring Data JPA sẽ tự hiểu: tìm theo User -> UserId và CreatedDate
    List<DailyQuest> findByUser_UserIdAndCreatedDate(Integer userId, LocalDate createdDate);
}