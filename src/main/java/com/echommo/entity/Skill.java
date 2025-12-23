package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "skills")
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Integer skillId;

    private String name;
    private String description;
    private String type; // ATTACK, HEAL

    @Column(name = "mana_cost")
    private Integer manaCost;

    private Integer power; // Dmg or Heal amount

    @Column(name = "required_level")
    private Integer requiredLevel;

    @Column(name = "image_url")
    private String imageUrl;
}