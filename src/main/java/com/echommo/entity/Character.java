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
    @Column(name = "char_id")
    private Integer charId;

    @Column(unique = true, nullable = false) // <--- Thêm trường Name
    private String name;

    // Liên kết 1-1 với User
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // --- Cấp độ & Kinh nghiệm ---
    @Column(name = "level")
    private Integer lv = 1; // Sẽ dùng setLv/getLv

    @Column(name = "current_exp")
    private Integer exp = 0;

    // --- Chỉ số Chiến đấu (Base Stats) ---
    @Column(name = "base_atk")
    private Integer baseAtk = 10;

    @Column(name = "base_def")
    private Integer baseDef = 5;

    @Column(name = "base_speed")
    private Integer baseSpeed = 10;

    // [FIX] Thêm chỉ số Crit Rate/Dmg
    @Column(name = "base_crit_rate")
    private Integer baseCritRate = 0;

    @Column(name = "base_crit_dmg")
    private Integer baseCritDmg = 150;

    // --- HP & Energy (Chân Khí) ---
    @Column(name = "max_hp")
    private Integer maxHp = 100;

    @Column(name = "current_hp")
    private Integer hp = 100;

    @Column(name = "max_energy")
    private Integer maxEnergy = 50;

    @Column(name = "current_energy")
    private Integer energy = 50;

    // --- Cấu hình Khám phá ---
    @Column(name = "current_location")
    private String currentLocation = "Sảnh Chính";
}