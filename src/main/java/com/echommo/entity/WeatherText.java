package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "weather_text")
public class WeatherText {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer wtId;

    @Column(nullable = false)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}