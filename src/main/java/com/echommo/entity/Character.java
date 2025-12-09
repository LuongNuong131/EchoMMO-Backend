package com.echommo.entity;

import com.echommo.enums.CharacterStatus; // [FIX] Import Enum
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@Table(name = "characters")
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer characterId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    @ToString.Exclude
    private User user;

    private String name;

    @Column(name = "character_class")
    private String characterClass = "Nhà Thám Hiểm";

    // 👇 [FIX] Thêm trạng thái nhân vật (IDLE, BATTLE...)
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CharacterStatus status = CharacterStatus.IDLE;

    private Integer level = 1;
    private Integer currentExp = 0;

    private Integer maxHp = 100;
    private Integer currentHp = 100;

    private Integer maxEnergy = 100;
    private Integer currentEnergy = 100;

    private Integer baseAtk = 10;
    private Integer baseDef = 5;

    @Column(name = "base_speed", columnDefinition = "int default 10")
    private Integer baseSpeed = 10;

    // Các chỉ số phụ
    private Integer baseCritRate = 50;
    private Integer baseCritDmg = 50;

    private Integer statPoints = 0;

    // 4 chỉ số cơ bản
    @Column(columnDefinition = "int default 5")
    private Integer str = 5;

    @Column(columnDefinition = "int default 5")
    private Integer dex = 5;

    @Column(columnDefinition = "int default 5")
    private Integer intelligence = 5;

    @Column(columnDefinition = "int default 5")
    private Integer luck = 5;

    public Character() {}
}