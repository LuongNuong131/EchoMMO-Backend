package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "enemies")
public class Enemy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enemy_id")
    private Integer enemyId;

    private String name;
    private Integer level;
    private Integer hp;
    private Integer atk;
    private Integer def;

    @Column(name = "exp_reward")
    private Integer expReward;

    @Column(name = "gold_reward")
    private Integer goldReward;

    @Column(name = "image_url")
    private String imageUrl;
}