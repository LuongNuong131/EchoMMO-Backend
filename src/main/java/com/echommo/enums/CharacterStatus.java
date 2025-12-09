package com.echommo.enums;

public enum CharacterStatus {
    IDLE,       // Đang rảnh
    EXPLORING,  // Đang đi map
    IN_COMBAT,     // Đang đánh nhau
    RESTING,     // Rảnh rỗi
    BATTLE,     // Đang chiến đấu
    WORKING,    // Đang làm việc (đào khoáng, câu cá...)
    DEAD// Thêm trạng thái nghỉ ngơi (nếu cần cho tính năng hồi phục sau này)
}