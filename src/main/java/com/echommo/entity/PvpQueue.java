package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "pvp_queue")
@Data
public class PvpQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer queueId;

    private Integer charId;
    private Integer level;
    private Integer power;
    private String status;
    private LocalDateTime joinedAt;
}