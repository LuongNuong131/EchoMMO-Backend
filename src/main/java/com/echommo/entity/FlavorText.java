package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "flavor_text")
public class FlavorText {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ftId;

    @Column(nullable = false)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}