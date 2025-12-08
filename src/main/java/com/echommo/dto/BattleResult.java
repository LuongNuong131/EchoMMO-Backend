package com.echommo.dto;

import com.echommo.entity.Enemy;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class BattleResult {
    // Thông tin hiển thị cơ bản
    private Integer playerHp;
    private Integer playerMaxHp;
    private Integer playerEnergy;

    private Integer enemyId; // [NEW] Chỉ trả về ID để FE render ảnh
    private String enemyName;
    private Integer enemyHp;
    private Integer enemyMaxHp;

    private List<String> combatLog = new ArrayList<>();
    private String status; // ONGOING, VICTORY, DEFEAT, QTE_ACTION

    // [NEW] Cờ báo hiệu QTE
    private boolean qteTriggered; // True: Hiện nút Đỡ đòn
    private double qteDuration;   // Thời gian tồn tại nút (ms)

    private Integer goldEarned = 0;
    private Integer expEarned = 0;
    private boolean levelUp = false;

    // Item drop
    private String droppedItemName;
    private String droppedItemImage;
    private String droppedItemRarity;
}