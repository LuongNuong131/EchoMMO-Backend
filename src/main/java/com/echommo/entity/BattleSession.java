//package com.echommo.entity;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import java.time.LocalDateTime;
//
//@Data
//@Entity
//@Table(name = "battle_sessions")
//public class BattleSession {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @OneToOne
//    @JoinColumn(name = "user_id", unique = true, nullable = false)
//    private User user;
//
//    // Lưu thông tin Enemy (Snapshot để tránh Enemy gốc bị sửa đổi)
//    private Integer enemyId;
//    private String enemyName;
//    private Integer enemyMaxHp;
//    private Integer enemyCurrentHp;
//    private Integer enemyAtk;
//    private Integer enemyDef;
//    private Integer enemySpeed;
//
//    // Trạng thái nhân vật trong trận
//    private Integer playerCurrentHp;
//    private Integer playerMaxHp;
//    private Integer playerCurrentEnergy;
//
//    private Integer currentTurn = 0;
//
//    // Trạng thái QTE
//    private boolean isQteActive = false;
//    private LocalDateTime qteExpiryTime; // Thời gian hết hạn bấm nút
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt = LocalDateTime.now();
//}

package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "battle_sessions")
public class BattleSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private Integer enemyId;
    private String enemyName;
    private Integer enemyMaxHp;
    private Integer enemyCurrentHp;
    private Integer enemyAtk;
    private Integer enemyDef;
    private Integer enemySpeed;

    private Integer playerCurrentHp;
    private Integer playerMaxHp;
    private Integer playerCurrentEnergy;

    private Integer currentTurn = 0;

    private boolean isQteActive = false;
    private LocalDateTime qteExpiryTime;

    // [NEW] Anti-Cheat Fields
    private LocalDateTime lastActionTime = LocalDateTime.now();
    private Integer spamCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}