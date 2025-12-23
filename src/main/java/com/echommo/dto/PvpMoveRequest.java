package com.echommo.dto; // Hoặc com.echommo.dto

public class PvpMoveRequest {
    private Long matchId;
    private String move;

    // Getter & Setter (Bắt buộc để Spring đọc JSON)
    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    public String getMove() { return move; }
    public void setMove(String move) { this.move = move; }
}