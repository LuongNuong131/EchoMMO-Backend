package com.echommo.dto;

import com.echommo.entity.Enemy;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BattleResult {
<<<<<<< HEAD
//old
//    private Enemy enemy;
//    private Integer playerHp;
//    private Integer playerMaxHp;
//    private Integer enemyHp;
//    private Integer enemyMaxHp;
//
//    private List<String> combatLog = new ArrayList<>(); // Nhật ký trận đấu (VD: Bạn chém 50dmg)
//    private String status; // ONGOING (Đang đánh), VICTORY (Thắng), DEFEAT (Thua)
//
//    private Integer goldEarned = 0;
//    private Integer expEarned = 0;

    //new
=======
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
    private Enemy enemy;
    private Integer playerHp;
    private Integer playerMaxHp;
    private Integer enemyHp;
    private Integer enemyMaxHp;

<<<<<<< HEAD
    private List<String> combatLog = new ArrayList<>();
    private String status; // ONGOING, VICTORY, DEFEAT

    private Integer goldEarned = 0;
    private Integer expEarned = 0;

    // [MỚI] Thêm cờ này để báo hiệu lên cấp
    private boolean levelUp = false;
=======
    private List<String> combatLog = new ArrayList<>(); // Nhật ký trận đấu (VD: Bạn chém 50dmg)
    private String status; // ONGOING (Đang đánh), VICTORY (Thắng), DEFEAT (Thua)

    private Integer goldEarned = 0;
    private Integer expEarned = 0;
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
}