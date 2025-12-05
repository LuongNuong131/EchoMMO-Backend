package com.echommo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_items")
public class UserItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_item_id")
    private Integer userItemId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    private Integer quantity = 1;

    @Column(name = "is_equipped")
    private Boolean isEquipped = false;

    @Column(name = "enhance_level")
    private Integer enhanceLevel = 0;

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt = LocalDateTime.now();
}