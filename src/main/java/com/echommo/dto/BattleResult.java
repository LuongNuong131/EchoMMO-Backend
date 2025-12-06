//package com.echommo.dto;
//
//import com.echommo.entity.Enemy;
//import lombok.Data;
//import java.util.ArrayList;
//import java.util.List;
//
//@Data
//public class BattleResult {
//    private Enemy enemy;
//    private Integer playerHp;
//    private Integer playerMaxHp;
//    private Integer enemyHp;
//    private Integer enemyMaxHp;
//
//    private List<String> combatLog = new ArrayList<>();
//    private String status; // ONGOING, VICTORY, DEFEAT, DIED
//
//    private Integer goldEarned = 0;
//    private Integer expEarned = 0;
//
//    // Field quan trọng để Frontend hiện popup Level Up
//    private boolean levelUp = false;
//    // Field để báo lỗi nếu có (VD: Hết lượt, lỗi mạng)
//    private boolean error = false;
//    private String message; // Message ngắn gọn cho Log
//}

package com.echommo.dto;

import com.echommo.entity.Enemy;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class BattleResult {
    private Enemy enemy;
    private Integer playerHp;
    private Integer playerMaxHp;

    // [FIX] Thêm dòng này để sửa lỗi cannot find symbol
    private Integer playerEnergy;

    private Integer enemyHp;
    private Integer enemyMaxHp;

    private List<String> combatLog = new ArrayList<>();
    private String status; // ONGOING, VICTORY, DEFEAT, DIED

    private Integer goldEarned = 0;
    private Integer expEarned = 0;

    // Field quan trọng để Frontend hiện popup Level Up
    private boolean levelUp = false;
    // Field để báo lỗi nếu có (VD: Hết lượt, lỗi mạng)
    private boolean error = false;
    private String message; // Message ngắn gọn cho Log
}