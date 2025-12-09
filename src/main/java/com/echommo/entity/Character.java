package com.echommo.entity;

import com.echommo.enums.CharacterStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@Table(name = "characters")
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "char_id") // [FIX] Map đúng vào cột char_id trong database
    private Integer characterId;

    @OneToOne
    @JoinColumn(name = "user_id") // Đã fix từ bước trước
    @ToString.Exclude
    private User user;

    private String name;

    @Column(name = "character_class")
    private String characterClass = "Nhà Thám Hiểm";

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CharacterStatus status = CharacterStatus.IDLE;

    private Integer level = 1;
    private Integer currentExp = 0;

    @Column(name = "max_hp") // Thêm mapping cho chắc
    private Integer maxHp = 100;

    @Column(name = "current_hp")
    private Integer currentHp = 100;

    @Column(name = "max_energy")
    private Integer maxEnergy = 100;

    @Column(name = "current_energy")
    private Integer currentEnergy = 100;

    @Column(name = "base_atk")
    private Integer baseAtk = 10;

    @Column(name = "base_def")
    private Integer baseDef = 5;

    @Column(name = "base_speed", columnDefinition = "int default 10")
    private Integer baseSpeed = 10;

    // Các chỉ số phụ
    @Column(name = "base_crit_rate")
    private Integer baseCritRate = 50;

    @Column(name = "base_crit_dmg")
    private Integer baseCritDmg = 50;

    @Column(name = "stat_points")
    private Integer statPoints = 0;

    // 4 chỉ số cơ bản
    @Column(columnDefinition = "int default 5")
    private Integer str = 5;

    @Column(columnDefinition = "int default 5")
    private Integer dex = 5; // [FIX] Sửa 'agi' thành 'dex' nếu DB dùng dex, hoặc map @Column(name="agi")

    @Column(columnDefinition = "int default 5")
    private Integer intelligence = 5;

    @Column(columnDefinition = "int default 5")
    private Integer luck = 5; // DB SQL dùng agi, vit. Bạn nên check lại xem muốn dùng Str/Dex/Int/Luk hay Str/Vit/Agi.
    // Nếu SQL là Str, Vit, Agi mà Java là Str, Dex, Int, Luck -> sẽ bị lệch.
    // Tạm thời code này sẽ chạy được, Hibernate sẽ tự tạo thêm cột Dex, Int, Luck.

    public Character() {}
}