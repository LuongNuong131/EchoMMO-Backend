package com.echommo.dto;

import lombok.Data;

@Data
public class DumpRequest {
    private String type;   // "NORMAL" hoặc "SHARK"
    private int amount;    // Số lượng thả
}