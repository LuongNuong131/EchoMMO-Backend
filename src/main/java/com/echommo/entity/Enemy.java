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
    private Integer hp;
    private Integer atk;
    private Integer def;

    // 👇 CÁC CỘT MỚI BẮT BUỘC PHẢI CÓ
    @Column(columnDefinition = "int default 10")
    private Integer speed = 10;

    @Column(name = "exp_reward", columnDefinition = "int default 10")
    private Integer expReward = 10;

    @Column(name = "gold_reward", columnDefinition = "int default 10")
    private Integer goldReward = 10;

    // Hình ảnh quái (để hiển thị frontend sau này)
    @Column(name = "image_url")
    private String imageUrl;

    public Enemy() {}
}