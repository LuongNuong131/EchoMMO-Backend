package com.echommo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    private Integer senderId;
    private String senderName;
    private String avatarUrl;
    private String content;
    private String timestamp;
    private String role;
}