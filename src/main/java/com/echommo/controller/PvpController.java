package com.echommo.controller;

import com.echommo.dto.PvpMoveRequest;
import com.echommo.entity.Character;
import com.echommo.entity.PvpMatch;
import com.echommo.entity.PvpQueue;
import com.echommo.entity.User;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.PvpMatchRepository;
import com.echommo.repository.PvpQueueRepository;
import com.echommo.repository.UserRepository;
import com.echommo.service.CharacterService;
import com.echommo.service.PvpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pvp")
public class PvpController {

    @Autowired private PvpService pvpService;
    @Autowired private CharacterService charService;
    @Autowired private UserRepository userRepo;
    @Autowired private CharacterRepository charRepo;
    @Autowired private PvpMatchRepository matchRepo;
    @Autowired private PvpQueueRepository queueRepo;

    // ================= 1. LẤY TRẠNG THÁI (POLLING) =================
    @GetMapping("/status")
    public ResponseEntity<?> getMatchStatus(@AuthenticationPrincipal UserDetails userDetails) {
        // [QUAN TRỌNG] Gọi hàm này để xử lý ai AFK quá 30s sẽ bị xử thua/mất lượt
        pvpService.checkTimeouts();

        Character myChar = getCharacterFromUser(userDetails);
        if (myChar == null) return ResponseEntity.badRequest().body("Character not found");

        MatchResponse response = new MatchResponse();
        response.setMyId(myChar.getCharId());

        // 1. Kiểm tra xem đang có trận nào active không
        PvpMatch match = pvpService.getLatestMatchForUser(myChar.getCharId());
        if (match != null) {
            mapMatchToResponse(match, myChar.getCharId(), response);
            return ResponseEntity.ok(response);
        }

        // 2. Nếu không có trận, kiểm tra xem có đang tìm trận (Hàng chờ) không
        Optional<PvpQueue> queueOpt = queueRepo.findByCharId(myChar.getCharId());
        if (queueOpt.isPresent()) {
            response.setStatus("SEARCHING");
            return ResponseEntity.ok(response);
        }

        // 3. Không có gì cả -> Đang ở sảnh
        response.setStatus("LOBBY");
        return ResponseEntity.ok(response);
    }

    // ================= 2. TÌM TRẬN =================
    @PostMapping("/find")
    public ResponseEntity<?> findMatch(@AuthenticationPrincipal UserDetails userDetails) {
        Character myChar = getCharacterFromUser(userDetails);
        if (myChar == null) return ResponseEntity.badRequest().body("Character not found");

        try {
            pvpService.findOrCreateMatch(myChar.getCharId());
            return ResponseEntity.ok(Map.of("message", "Started searching", "status", "SEARCHING"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ================= 3. CHẤP NHẬN TRẬN ĐẤU =================
    @PostMapping("/accept")
    public ResponseEntity<?> acceptMatch(@AuthenticationPrincipal UserDetails userDetails,
                                         @RequestBody Map<String, Long> payload) {
        Character myChar = getCharacterFromUser(userDetails);
        Long matchId = payload.get("matchId");

        if (matchId == null) return ResponseEntity.badRequest().body("Match ID is required");

        try {
            pvpService.acceptMatch(matchId, myChar.getCharId());
            return ResponseEntity.ok(Map.of("message", "Accepted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ================= 4. RA CHIÊU (ROCK/PAPER/SCISSORS) =================
    @PostMapping("/move")
    public ResponseEntity<?> submitMove(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody PvpMoveRequest request) {
        Character myChar = getCharacterFromUser(userDetails);

        if (request.getMatchId() == null) return ResponseEntity.badRequest().body("Match ID is required");
        if (request.getMove() == null) return ResponseEntity.badRequest().body("Move is required");

        try {
            Optional<PvpMatch> matchOpt = matchRepo.findById(request.getMatchId());
            if (matchOpt.isPresent() && "FINISHED".equals(matchOpt.get().getStatus())) {
                return ResponseEntity.ok("Match already finished");
            }

            pvpService.submitMove(request.getMatchId(), myChar.getCharId(), request.getMove());
            return ResponseEntity.ok(Map.of("message", "Move submitted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ================= 5. CHAT TRONG TRẬN =================
    @PostMapping("/chat")
    public ResponseEntity<?> sendChat(@AuthenticationPrincipal UserDetails userDetails,
                                      @RequestBody Map<String, Object> payload) {
        Character myChar = getCharacterFromUser(userDetails);

        Object matchIdObj = payload.get("matchId");
        if (matchIdObj == null) return ResponseEntity.badRequest().body("Match ID required");

        Long matchId = ((Number) matchIdObj).longValue();
        String message = (String) payload.get("message");

        try {
            pvpService.saveChatMessage(matchId, myChar.getCharId(), message);
            return ResponseEntity.ok(Map.of("message", "Chat sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ================= 6. ĐẦU HÀNG =================
    @PostMapping("/surrender")
    public ResponseEntity<?> surrender(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestBody Map<String, Long> payload) {
        Character myChar = getCharacterFromUser(userDetails);
        Long matchId = payload.get("matchId");

        if (matchId == null) return ResponseEntity.badRequest().body("Match ID is required");

        try {
            pvpService.surrenderMatch(matchId, myChar.getCharId());
            return ResponseEntity.ok(Map.of("message", "Surrender accepted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ================= 7. HỦY TÌM TRẬN =================
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSearch(@AuthenticationPrincipal UserDetails userDetails) {
        Character myChar = getCharacterFromUser(userDetails);
        if (myChar == null) return ResponseEntity.badRequest().body("Character not found");
        try {
            pvpService.cancelQueue(myChar.getCharId());
            return ResponseEntity.ok(Map.of("message", "Search canceled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ================= 8. BẢNG XẾP HẠNG PVP =================
    @GetMapping("/leaderboard")
    public ResponseEntity<List<CharacterDTO>> getLeaderboard() {
        // Lấy top 10 người thắng nhiều nhất
        List<Character> topPlayers = charRepo.findTop10ByOrderByPvpWinsDesc();

        List<CharacterDTO> result = topPlayers.stream().map(c -> {
            CharacterDTO dto = new CharacterDTO();
            dto.setName(c.getName());
            dto.setLevel(c.getLevel());
            dto.setPvpWins(c.getPvpWins());
            dto.setReputation(c.getReputation()); // Thêm hiển thị danh vọng
            dto.setRankTitle(c.getRankTitle());   // Thêm hiển thị danh hiệu
            dto.setAvatarUrl(c.getAvatarUrl());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ================= HELPER METHODS =================

    private Character getCharacterFromUser(UserDetails userDetails) {
        if (userDetails == null) return null;
        User user = userRepo.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) return null;

        Character c = charRepo.findByUser_UserId(user.getUserId()).orElse(null);

        // TÍNH TOÁN LẠI STATS TỪ ĐỒ MỖI KHI GỌI API PVP
        // Để đảm bảo máu/damage luôn đúng với set đồ hiện tại
        if (c != null) {
            charService.recalculateStats(c);
        }
        return c;
    }

    private void mapMatchToResponse(PvpMatch match, Integer myCharId, MatchResponse res) {
        res.setMatchId(match.getMatchId());
        res.setStatus(match.getStatus());
        res.setTurnCount(match.getTurnCount());
        res.setLastLog(match.getLastLog());
        res.setWinnerId(match.getWinnerId() != null ? match.getWinnerId().intValue() : null);

        // Map lịch sử move
        res.setLastP1Move(match.getLastP1Move());
        res.setLastP2Move(match.getLastP2Move());

        // Map Chat
        if (match.getChats() != null) {
            List<ChatMessageDTO> chatDtos = match.getChats().stream()
                    .map(chat -> new ChatMessageDTO(chat.getSender().getName(), chat.getSender().getCharId(), chat.getMessage()))
                    .collect(Collectors.toList());
            res.setMessages(chatDtos);
        } else {
            res.setMessages(new ArrayList<>());
        }

        boolean isP1 = match.getPlayer1().getCharId().equals(myCharId);

        // Thông tin Player 1
        res.setP1Id(match.getPlayer1().getCharId());
        res.setP1Name(match.getPlayer1().getName());
        res.setP1Level(match.getPlayer1().getLevel());
        res.setP1Hp(match.getP1CurrentHp());
        res.setP1MaxHp(match.getPlayer1().getMaxHp()); // Max HP đã tính đồ
        res.setP1AvatarUrl(match.getPlayer1().getAvatarUrl());

        // Thông tin Player 2
        res.setP2Id(match.getPlayer2().getCharId());
        res.setP2Name(match.getPlayer2().getName());
        res.setP2Level(match.getPlayer2().getLevel());
        res.setP2Hp(match.getP2CurrentHp());
        res.setP2MaxHp(match.getPlayer2().getMaxHp()); // Max HP đã tính đồ
        res.setP2AvatarUrl(match.getPlayer2().getAvatarUrl());

        // Logic ẩn/hiện nước đi
        String p1Move = match.getP1Move();
        String p2Move = match.getP2Move();

        // Nếu trận đấu đã kết thúc hoặc cả 2 đã ra đòn -> Show hết
        boolean showAll = "FINISHED".equals(match.getStatus());
        boolean bothMoved = (p1Move != null && p2Move != null);

        if (isP1) {
            // Mình là P1: Thấy move của mình. Move của địch chỉ thấy khi nó đã đánh xong turn hoặc game over
            res.setP1Move(p1Move);
            res.setP2Move((showAll || bothMoved) ? p2Move : (p2Move != null ? "HIDDEN" : null));
        } else {
            // Mình là P2
            res.setP2Move(p2Move);
            res.setP1Move((showAll || bothMoved) ? p1Move : (p1Move != null ? "HIDDEN" : null));
        }
    }

    // ================= DTO STATIC CLASSES =================
    // Để chung trong Controller cho gọn, hoặc tách ra file riêng tùy ông

    public static class ChatMessageDTO {
        public String senderName; public Integer senderId; public String content;
        public ChatMessageDTO(String s, Integer id, String c) { this.senderName = s; this.senderId = id; this.content = c; }
    }

    public static class CharacterDTO {
        private String name;
        private Integer level;
        private Integer pvpWins;
        private Integer reputation; // Danh vọng
        private String rankTitle;   // Danh hiệu
        private String avatarUrl;

        // Getters Setters
        public String getName() { return name; } public void setName(String name) { this.name = name; }
        public Integer getLevel() { return level; } public void setLevel(Integer level) { this.level = level; }
        public Integer getPvpWins() { return pvpWins; } public void setPvpWins(Integer pvpWins) { this.pvpWins = pvpWins; }
        public Integer getReputation() { return reputation; } public void setReputation(Integer reputation) { this.reputation = reputation; }
        public String getRankTitle() { return rankTitle; } public void setRankTitle(String rankTitle) { this.rankTitle = rankTitle; }
        public String getAvatarUrl() { return avatarUrl; } public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    public static class MatchResponse {
        private String status;
        private Long matchId;
        private Integer myId;
        private Integer winnerId;
        private Integer turnCount;
        private String lastLog;

        private Integer p1Id; private String p1Name; private Integer p1Level;
        private Integer p1Hp; private Integer p1MaxHp; private String p1Move;
        private String p1AvatarUrl;

        private Integer p2Id; private String p2Name; private Integer p2Level;
        private Integer p2Hp; private Integer p2MaxHp; private String p2Move;
        private String p2AvatarUrl;

        private String lastP1Move;
        private String lastP2Move;

        private List<ChatMessageDTO> messages;

        // Getters and Setters
        public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
        public Long getMatchId() { return matchId; } public void setMatchId(Long matchId) { this.matchId = matchId; }
        public Integer getMyId() { return myId; } public void setMyId(Integer myId) { this.myId = myId; }
        public Integer getWinnerId() { return winnerId; } public void setWinnerId(Integer winnerId) { this.winnerId = winnerId; }
        public Integer getTurnCount() { return turnCount; } public void setTurnCount(Integer turnCount) { this.turnCount = turnCount; }
        public String getLastLog() { return lastLog; } public void setLastLog(String lastLog) { this.lastLog = lastLog; }

        public Integer getP1Id() { return p1Id; } public void setP1Id(Integer p1Id) { this.p1Id = p1Id; }
        public String getP1Name() { return p1Name; } public void setP1Name(String p1Name) { this.p1Name = p1Name; }
        public Integer getP1Level() { return p1Level; } public void setP1Level(Integer p1Level) { this.p1Level = p1Level; }
        public Integer getP1Hp() { return p1Hp; } public void setP1Hp(Integer p1Hp) { this.p1Hp = p1Hp; }
        public Integer getP1MaxHp() { return p1MaxHp; } public void setP1MaxHp(Integer p1MaxHp) { this.p1MaxHp = p1MaxHp; }
        public String getP1Move() { return p1Move; } public void setP1Move(String p1Move) { this.p1Move = p1Move; }
        public String getP1AvatarUrl() { return p1AvatarUrl; } public void setP1AvatarUrl(String p1AvatarUrl) { this.p1AvatarUrl = p1AvatarUrl; }

        public Integer getP2Id() { return p2Id; } public void setP2Id(Integer p2Id) { this.p2Id = p2Id; }
        public String getP2Name() { return p2Name; } public void setP2Name(String p2Name) { this.p2Name = p2Name; }
        public Integer getP2Level() { return p2Level; } public void setP2Level(Integer p2Level) { this.p2Level = p2Level; }
        public Integer getP2Hp() { return p2Hp; } public void setP2Hp(Integer p2Hp) { this.p2Hp = p2Hp; }
        public Integer getP2MaxHp() { return p2MaxHp; } public void setP2MaxHp(Integer p2MaxHp) { this.p2MaxHp = p2MaxHp; }
        public String getP2Move() { return p2Move; } public void setP2Move(String p2Move) { this.p2Move = p2Move; }
        public String getP2AvatarUrl() { return p2AvatarUrl; } public void setP2AvatarUrl(String p2AvatarUrl) { this.p2AvatarUrl = p2AvatarUrl; }

        public String getLastP1Move() { return lastP1Move; } public void setLastP1Move(String lastP1Move) { this.lastP1Move = lastP1Move; }
        public String getLastP2Move() { return lastP2Move; } public void setLastP2Move(String lastP2Move) { this.lastP2Move = lastP2Move; }

        public List<ChatMessageDTO> getMessages() { return messages; } public void setMessages(List<ChatMessageDTO> messages) { this.messages = messages; }
    }
}