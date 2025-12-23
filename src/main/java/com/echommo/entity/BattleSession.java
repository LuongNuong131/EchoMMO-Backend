package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime; // [QUAN TRỌNG] Import này để dùng LocalDateTime

@Entity
@Data
@NoArgsConstructor
@Table(name = "battle_sessions")
public class BattleSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "char_id")
    private Character character;

    // --- Thông tin Quái (Snapshot) ---
    @Column(name = "enemy_id")
    private Integer enemyId;

    @Column(name = "enemy_name")
    private String enemyName;

    @Column(name = "enemy_max_hp")
    private Integer enemyMaxHp;

    @Column(name = "enemy_current_hp")
    private Integer enemyCurrentHp;

    @Column(name = "enemy_atk")
    private Integer enemyAtk;

    @Column(name = "enemy_def")
    private Integer enemyDef;

    @Column(name = "enemy_speed")
    private Integer enemySpeed;

    // --- Thông tin Người chơi (Snapshot) ---
    @Column(name = "player_max_hp")
    private Integer playerMaxHp;

    @Column(name = "player_current_hp")
    private Integer playerCurrentHp;

    @Column(name = "player_current_energy")
    private Integer playerCurrentEnergy;

    // --- Trạng thái trận đấu ---
    @Column(name = "current_turn")
    private Integer currentTurn = 0;

    @Column(columnDefinition = "TEXT")
    private String log;

    // [FIX] Thêm trường này để hết lỗi "cannot find symbol setCreatedAt"
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // --- Helper Methods ---
    public void setEnemy(Enemy e) {
        this.enemyId = e.getEnemyId();
        this.enemyName = e.getName();
        this.enemyMaxHp = e.getHp();
        this.enemyCurrentHp = e.getHp();
        this.enemyAtk = e.getAtk();
        this.enemyDef = e.getDef();
        this.enemySpeed = e.getSpeed();
    }
}