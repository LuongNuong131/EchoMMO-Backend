package com.echommo.repository;

import com.echommo.entity.DailyQuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyQuestRepository extends JpaRepository<DailyQuest, Integer> {
    // Lấy list nhiệm vụ của user trong ngày hôm nay
    List<DailyQuest> findByUser_UserIdAndCreatedDate(Integer userId, LocalDate date);
}