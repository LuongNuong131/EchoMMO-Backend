package com.echommo.entity;

import com.echommo.enums.CharacterStatus; // Giữ nguyên enum cũ của bạn nếu có
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

    private Integer level;

    @Column(name = "current_exp")
    private Integer currentExp;

    // --- Attribute Points (Mới) ---
    @Column(name = "stat_points")
    private Integer statPoints;

    private Integer str;
    private Integer vit;
    private Integer agi;

    // --- Snapshot Stats ---
    @Column(name = "current_hp")
    private Integer currentHp;

    @Column(name = "max_hp")
    private Integer maxHp;

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

    // Giữ các field cũ nếu logic game cũ cần, nhưng map với DB mới thì nhiêu đây là đủ core.
}