package com.echommo.dto;

public class ReportRequest {
    private String title;
    private String description;

    // 1. Constructor mặc định (Bắt buộc để Jackson đọc JSON)
    public ReportRequest() {
    }

    // 2. Constructor đầy đủ (Tùy chọn)
    public ReportRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // 3. Getters và Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}