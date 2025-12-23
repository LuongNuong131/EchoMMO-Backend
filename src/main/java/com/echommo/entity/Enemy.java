package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "enemies")
public class Enemy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer enemyId;

    private String name;

    @Column(columnDefinition = "int default 1")
    private Integer level = 1;

    private Integer hp;
    private Integer atk;
    private Integer def;

    @Column(columnDefinition = "int default 10")
    private Integer speed = 10;

    @Column(name = "exp_reward", columnDefinition = "int default 10")
    private Integer expReward = 10;

    @Column(name = "gold_reward", columnDefinition = "int default 10")
    private Integer goldReward = 10;

    @Column(name = "image_url")
    private String imageUrl;

    public Enemy() {}

    // [FIX] Thêm getter thủ công cho chắc chắn
    public Integer getEnemyId() {
        return enemyId;
    }

    public void setEnemyId(Integer enemyId) {
        this.enemyId = enemyId;
    }
}