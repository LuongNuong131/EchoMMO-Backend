package com.echommo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "characters")
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "char_id") // <--- Quan trọng: Khớp với DB
    private Integer charId;

    @Column(unique = true, nullable = false)
    private String name;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "level")
    private Integer lv = 1;

    @Column(name = "current_exp")
    private Integer exp = 0;

    // Stats
    @Column(name = "base_atk")
    private Integer baseAtk = 10;
    @Column(name = "base_def")
    private Integer baseDef = 5;
    @Column(name = "base_speed")
    private Integer baseSpeed = 10;
    @Column(name = "base_crit_rate")
    private Integer baseCritRate = 5;
    @Column(name = "base_crit_dmg")
    private Integer baseCritDmg = 150;

    @Column(name = "max_hp")
    private Integer maxHp = 100;
    @Column(name = "current_hp")
    private Integer hp = 100;

    @Column(name = "max_energy")
    private Integer maxEnergy = 50;
    @Column(name = "current_energy")
    private Integer energy = 50;

    @Column(name = "current_location")
    private String currentLocation = "Làng Tân Thủ";
}