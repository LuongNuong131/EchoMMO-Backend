package com.echommo.entity;

import com.echommo.enums.CharacterStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "characters")
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "char_id")
    private Integer charId;

    // [FIX] Đổi từ @ManyToOne thành @OneToOne để khớp với bên User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true) // Thêm unique=true
    @JsonIgnore
    private User user;

    @Column(nullable = false, unique = true)
    private String name;

    private Integer level = 1;
    private Integer currentExp = 0;

    @Column(name = "character_class")
    private String characterClass = "Nhà Thám Hiểm";

    @Enumerated(EnumType.STRING)
    private CharacterStatus status = CharacterStatus.IDLE;

    // --- MAIN STATS ---
    private Integer statPoints = 0;
    private Integer str = 5;
    private Integer vit = 5;
    private Integer agi = 5;
    private Integer dex = 5;
    private Integer intelligence = 5;
    private Integer luck = 5;

    // --- COMPUTED STATS ---
    private Integer currentHp = 100;
    private Integer maxHp = 100;
    private Integer currentEnergy = 50;
    private Integer maxEnergy = 50;

    private Integer baseAtk = 10;
    private Integer baseDef = 5;
    private Integer baseSpeed = 10;

    private Integer baseCritRate = 50; // 5%
    private Integer baseCritDmg = 150; // 150%

    private String currentLocation = "Làng Tân Thủ";

    @Column(name = "spa_start_time")
    private java.time.LocalDateTime spaStartTime;

    @Column(name = "spa_end_time")
    private java.time.LocalDateTime spaEndTime;

    @Column(name = "spa_package_type")
    private String spaPackageType;
}