package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "pvp_match")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PvpMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchId;

    @Column(name = "status")
    private String status; // PENDING (Chờ chấp nhận), ACTIVE (Đang đánh), FINISHED (Kết thúc)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Cực kỳ quan trọng: Dùng để tính toán thời gian hết hiệp (30s).
     * Backend sẽ dựa vào mốc này để trừ máu nếu người chơi không ra chiêu.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "turn_count")
    private int turnCount;

    @Column(name = "last_log", columnDefinition = "TEXT")
    private String lastLog;

    @Column(name = "winner_id")
    private Long winnerId;

    // --- PLAYER 1 INFO ---
    @ManyToOne
    @JoinColumn(name = "player1_id")
    private Character player1;

    @Column(name = "p1_current_hp")
    private Integer p1CurrentHp;

    @Column(name = "p1_move")
    private String p1Move; // Nước đi hiện tại (ROCK, PAPER, SCISSORS)

    @Column(name = "p1_accepted")
    private boolean p1Accepted;

    @Column(name = "last_p1_move")
    private String lastP1Move; // Lưu lại nước đi hiệp trước để hiện animation

    // --- PLAYER 2 INFO ---
    @ManyToOne
    @JoinColumn(name = "player2_id")
    private Character player2;

    @Column(name = "p2_current_hp")
    private Integer p2CurrentHp;

    @Column(name = "p2_move")
    private String p2Move; // Nước đi hiện tại

    @Column(name = "p2_accepted")
    private boolean p2Accepted;

    @Column(name = "last_p2_move")
    private String lastP2Move; // Lưu lại nước đi hiệp trước

    // --- CHAT & AUDIT ---
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PvpChat> chats = new ArrayList<>();

    /**
     * Tự động cập nhật mốc thời gian mỗi khi dữ liệu thay đổi.
     * Giúp hệ thống đếm ngược 30s chính xác cho mỗi lượt đánh.
     */
    @PreUpdate
    @PrePersist
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}