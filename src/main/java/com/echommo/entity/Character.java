package com.echommo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "characters")
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_id")
    private Integer characterId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String name;

    private Integer level = 1;

    @Column(name = "current_exp")
    private Long currentExp = 0L;

    // --- BASE STATS ---
    private Integer hp = 100;

    @Column(name = "max_hp")
    private Integer maxHp = 100;

    private Integer atk = 10;
    private Integer def = 5;

    // LƯU Ý: Tên biến là 'speed', method setter sẽ là 'setSpeed'
    private Integer speed = 10;

    @Column(name = "crit_rate")
    private Integer critRate = 5; // 5%

    @Column(name = "crit_dmg")
    private Integer critDmg = 150; // 150%

    private Integer energy = 50;

    @Column(name = "max_energy")
    private Integer maxEnergy = 50;

    @Column(name = "stat_points")
    private Integer statPoints = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}