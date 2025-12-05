package com.echommo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessageDTO {
    private String senderName;
    private String content;
    private String time;
    private String role; // Để hiển thị màu Admin cho ngầu
}