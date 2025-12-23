package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date; // Service đang setTimestamp bằng Date

@Entity
@Table(name = "pvp_chat")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PvpChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    // --- Quan hệ ---

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private PvpMatch match;

    @ManyToOne
    @JoinColumn(name = "sender_id") // Người gửi tin nhắn
    private Character sender;
}