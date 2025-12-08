package com.echommo.entity;

import com.echommo.enums.CharacterStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "characters")
@Data
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "char_id")
    private Integer charId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, unique = true)
    private String name;

    // [FIX] Đổi tên biến lv -> level để khớp với Service
    private Integer level;

    // [FIX] Đổi tên biến exp -> currentExp
    @Column(name = "current_exp")
    private Integer currentExp;

    // --- Hệ thống điểm tiềm năng (Mới) ---
    @Column(name = "stat_points")
    private Integer statPoints;

    private Integer str;
    private Integer vit;
    private Integer agi;

    // [FIX] Đổi tên hp -> currentHp
    @Column(name = "current_hp")
    private Integer currentHp;

    @Column(name = "max_hp")
    private Integer maxHp;

    // [FIX] Đổi tên energy -> currentEnergy
    @Column(name = "current_energy")
    private Integer currentEnergy;

    @Column(name = "max_energy")
    private Integer maxEnergy;

    // Base Stats
    @Column(name = "base_atk")
    private Integer baseAtk;

    @Column(name = "base_def")
    private Integer baseDef;

    @Column(name = "base_speed")
    private Integer baseSpeed;

    @Column(name = "base_crit_rate")
    private Integer baseCritRate;

    @Column(name = "base_crit_dmg")
    private Integer baseCritDmg;

    @Column(name = "current_location")
    private String currentLocation;

    // [FIX QUAN TRỌNG] Thêm lại trường status bị thiếu
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CharacterStatus status;
}