package com.echommo.service;

import com.echommo.dto.ExplorationResponse;
import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.CharacterStatus;
import com.echommo.enums.SlotType;
import com.echommo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExplorationService {

    private final CharacterRepository characterRepository;
    private final WalletRepository walletRepository;
    private final CaptchaService captchaService;
    private final ItemRepository itemRepo;
    private final UserItemRepository userItemRepo;
    private final FlavorTextRepository flavorTextRepo;
    private final ItemGenerationService itemGenService;
    private final EnemyRepository enemyRepository;
    private final BattleSessionRepository battleSessionRepo;
    private final CharacterService characterService;

    // Map chống Spam Click (500ms)
    private final Map<Integer, Long> lastActionMap = new HashMap<>();

    // ... (Giữ nguyên enum GameMap)
    public enum GameMap {
        MAP_01("MAP_01", "Đồng Bằng", 1, 19,
                createWeightedList(Map.of("w_wood", 40, "o_coal", 30, "o_copper", 20, "f_fish", 10)),
                List.of("Slime Xanh", "Thỏ Điên", "Sói Hoang", "Goblin Trinh Sát")),

        MAP_02("MAP_02", "Rừng Rậm", 20, 29,
                createWeightedList(Map.of("w_wood", 30, "o_copper", 20, "o_iron", 20, "f_fish", 30)),
                List.of("Nhện Độc", "Gấu Xám", "Tinh Linh Rừng")),

        MAP_03("MAP_03", "Sa Mạc", 30, 39,
                createWeightedList(Map.of("w_woodRed", 30, "GOLD_MINE_SPECIAL", 40, "o_iron", 20, "o_gold", 10)),
                List.of("Bọ Cạp Cát", "Rắn Đuôi Chuông", "Mummy")),

        MAP_04("MAP_04", "Núi Cao", 40, 49,
                createWeightedList(Map.of("o_coal", 30, "o_iron", 30, "o_platinum", 20, "w_woodWhite", 15, "o_gold", 5)),
                List.of("Golem Đá", "Đại Bàng Núi", "Rồng Đá Nhỏ")),

        MAP_05("MAP_05", "Băng Đảo", 50, 59,
                createWeightedList(Map.of("w_woodWhite", 40, "o_platinum", 40, "f_whiteshark", 15, "o_gold", 5)),
                List.of("Sói Tuyết", "Yeti Khổng Lồ", "Phù Thủy Băng")),

        MAP_06("MAP_06", "Vùng Đất Chết", 60, 70,
                createWeightedList(Map.of("w_woodBlack", 30, "o_strange", 30, "f_megalodon", 20, "r_coinEcho", 15, "o_gold", 5)),
                List.of("Bóng Ma", "Hiệp Sĩ Tử Vong", "Lich King"));

        public final String id; public final String name;
        public final int minLv; public final int maxLv;
        public final List<String> resourceCodes;
        public final List<String> enemies;

        GameMap(String id, String name, int minLv, int maxLv, List<String> resourceCodes, List<String> enemies) {
            this.id = id; this.name = name; this.minLv = minLv; this.maxLv = maxLv;
            this.resourceCodes = resourceCodes; this.enemies = enemies;
        }

        private static List<String> createWeightedList(Map<String, Integer> weights) {
            List<String> list = new ArrayList<>();
            weights.forEach((code, count) -> { for(int i=0; i<count; i++) list.add(code); });
            return list;
        }
        public static GameMap findById(String id) {
            return Arrays.stream(values()).filter(m -> m.id.equalsIgnoreCase(id)).findFirst().orElse(MAP_01);
        }
    }

    @Transactional
    public ExplorationResponse explore(User user, String mapId) {
        long now = System.currentTimeMillis();
        if (now - lastActionMap.getOrDefault(user.getUserId(), 0L) < 500) {
            throw new RuntimeException("Thao tác quá nhanh!");
        }
        lastActionMap.put(user.getUserId(), now);

        Character c = characterRepository.findByUser_UserIdWithUserAndWallet(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa có nhân vật!"));

        captchaService.checkLockStatus(c.getUser());

        if (c.getCurrentEnergy() < 1) throw new RuntimeException("Hết năng lượng! Hãy vào Spa nghỉ ngơi.");
        c.setCurrentEnergy(c.getCurrentEnergy() - 1);

        GameMap map = GameMap.findById(mapId);
        if (c.getLevel() < map.minLv) throw new RuntimeException("Cấp độ không đủ! Cần level " + map.minLv);

        Random r = new Random();
        Wallet w = c.getUser().getWallet();

        // 1. TÍNH TOÁN LƯƠNG CỨNG (Exp + Gold)
        long expGained = 10L + c.getLevel();
        c.setCurrentExp(c.getCurrentExp() + expGained);

        int baseGold = 10 + c.getLevel();
        int randomGold = r.nextInt(5 + c.getLevel() / 2);
        BigDecimal totalGoldGained = BigDecimal.valueOf(baseGold + randomGold);

        int roll = r.nextInt(100);
        String type; String msg;
        String rewardName = null;
        String rewardItemCode = null;
        Integer rewardAmount = 0; Integer rewardItemId = null;

        if (roll < 70) {
            type = "TEXT";
            msg = flavorTextRepo.findRandomContent().orElse("Bạn đi dạo quanh " + map.name + ".");
            clearGatheringState(c);
        } else if (roll < 81) {
            type = "GATHERING";
            String resCode = map.resourceCodes.get(r.nextInt(map.resourceCodes.size()));

            if ("GOLD_MINE_SPECIAL".equals(resCode)) {
                // Rơi vào mỏ vàng -> Cộng thêm tiền thưởng
                BigDecimal bonusGold = BigDecimal.valueOf(50 + r.nextInt(150));
                totalGoldGained = totalGoldGained.add(bonusGold);

                // MSG chỉ hiện text, không hiện số tiền (FE sẽ hiện số tiền)
                msg = "Tìm thấy kho báu bí mật!";
                type = "GOLD_MINE";
                clearGatheringState(c);
            } else {
                Item item = itemRepo.findByCode(resCode).orElse(null);
                if (item != null) {
                    int amount = 10 + r.nextInt(21);
                    c.setGatheringItemId(item.getItemId());
                    c.setGatheringRemainingAmount(amount);
                    c.setGatheringExpiry(LocalDateTime.now().plusMinutes(3));

                    if ("o_gold".equals(item.getCode())) {
                        msg = "✨ TÀI LỘC! Phát hiện mạch " + item.getName() + " lấp lánh!";
                    } else {
                        msg = "Phát hiện bãi " + item.getName();
                    }
                    rewardName = item.getName();
                    rewardItemCode = item.getCode();
                    rewardAmount = amount;
                    rewardItemId = item.getItemId();
                } else {
                    type = "TEXT"; msg = "Khu vực trống trải."; clearGatheringState(c);
                }
            }
        } else if (roll < 91) {
            type = "ENEMY";
            String enemyName = map.enemies.get(r.nextInt(map.enemies.size()));
            Enemy baseEnemy = enemyRepository.findByName(enemyName)
                    .orElseGet(() -> enemyRepository.findAll().stream().findFirst().orElseThrow(() -> new RuntimeException("Dữ liệu quái lỗi!")));

            boolean isElite = r.nextInt(100) < 20;
            createScaledBattleSession(c, baseEnemy, isElite);

            String prefix = isElite ? "💀 [Tinh Anh] " : "";
            msg = "Đụng độ " + prefix + enemyName + " (Lv." + baseEnemy.getLevel() + ")!";
            clearGatheringState(c);
        } else {
            type = "ITEM";
            String code = map.resourceCodes.get(r.nextInt(map.resourceCodes.size()));
            if ("GOLD_MINE_SPECIAL".equals(code)) {
                BigDecimal bonus = BigDecimal.valueOf(20);
                totalGoldGained = totalGoldGained.add(bonus);
                msg = "Nhặt được túi vàng rơi.";
            } else {
                Item it = itemRepo.findByCode(code).orElse(null);
                if (it != null) {
                    addItemToInventory(c, it, 1);

                    if ("o_gold".equals(it.getCode())) {
                        msg = "✨ MAY MẮN! Nhặt được " + it.getName() + " quý giá!";
                    } else {
                        msg = "Nhặt được 1 " + it.getName();
                    }
                    rewardName = it.getName();
                    rewardItemCode = it.getCode();
                    rewardAmount = 1;
                } else {
                    msg = "Không tìm thấy gì.";
                }
            }
            clearGatheringState(c);
        }

        // Lưu tổng Gold
        w.setGold(w.getGold().add(totalGoldGained));

        checkExploreLevelUp(c);
        characterRepository.save(c);
        walletRepository.save(w);

        return ExplorationResponse.builder()
                .message(msg).type(type)
                .currentLv(c.getLevel()).currentExp(c.getCurrentExp())
                .currentEnergy(c.getCurrentEnergy()).maxEnergy(c.getMaxEnergy())
                .rewardName(rewardName)
                .rewardItemCode(rewardItemCode)
                .rewardAmount(rewardAmount).rewardItemId(rewardItemId)
                .goldGained(totalGoldGained) // Trả về tổng vàng (Lương cứng + Bonus)
                .build();
    }

    // ... (Giữ nguyên các hàm private bên dưới)
    // createScaledBattleSession, gatherResource, etc.
    private void createScaledBattleSession(Character player, Enemy enemy, boolean isElite) {
        BattleSession session = battleSessionRepo.findByCharacter_CharId(player.getCharId())
                .stream().findFirst().orElse(new BattleSession());

        if (session.getId() == null) {
            session.setCharacter(player);
        }

        int lvl = enemy.getLevel() != null ? enemy.getLevel() : 1;
        double levelScaling = 1 + (player.getLevel() > lvl ? (player.getLevel() - lvl) * 0.05 : 0);
        double hpMult = isElite ? 2.5 : 1.0;
        double statMult = isElite ? 1.5 : 1.0;

        int scaledHp = (int) (enemy.getHp() * levelScaling * hpMult);
        int scaledAtk = (int) (enemy.getAtk() * statMult);
        int scaledDef = (int) (enemy.getDef() * statMult);

        session.setEnemyId(enemy.getEnemyId());
        String nameDisplay = (isElite ? "💀 [Tinh Anh] " : "") + enemy.getName();
        session.setEnemyName(nameDisplay);
        session.setEnemyMaxHp(scaledHp);
        session.setEnemyCurrentHp(scaledHp);
        session.setEnemyAtk(scaledAtk);
        session.setEnemyDef(scaledDef);
        session.setEnemySpeed(enemy.getSpeed());
        session.setPlayerMaxHp(player.getMaxHp());
        session.setPlayerCurrentHp(player.getCurrentHp());
        session.setCurrentTurn(0);
        session.setCreatedAt(LocalDateTime.now());

        battleSessionRepo.save(session);
        player.setStatus(CharacterStatus.IN_COMBAT);
    }

    // ... Copy nốt các hàm gatherResource, determineRequiredTool, v.v. từ code cũ của bạn
    @Transactional
    public Map<String, Object> gatherResource(User user, int itemId, int amountRequest) {
        // ... (Logic cũ giữ nguyên)
        Character c = characterRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Character not found"));

        if (c.getGatheringItemId() == null || c.getGatheringItemId() != itemId)
            throw new RuntimeException("Mỏ tài nguyên không hợp lệ!");
        if (c.getGatheringExpiry() != null && LocalDateTime.now().isAfter(c.getGatheringExpiry())) {
            clearGatheringState(c); characterRepository.save(c);
            throw new RuntimeException("Mỏ đã sập!");
        }
        if (c.getGatheringRemainingAmount() < amountRequest)
            throw new RuntimeException("Trữ lượng không đủ!");

        Item resourceItem = itemRepo.findById(itemId).orElseThrow();

        SlotType requiredToolType = determineRequiredTool(resourceItem.getCode());

        UserItem equippedTool = userItemRepo.findByCharacter_CharIdAndIsEquippedTrue(c.getCharId()).stream()
                .filter(ui -> ui.getItem().getSlotType() == requiredToolType)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Bạn cần trang bị " + getToolName(requiredToolType) + " để khai thác!"));

        if (equippedTool.getCurrentDurability() != null && equippedTool.getCurrentDurability() <= 0) {
            throw new RuntimeException("Dụng cụ đã hỏng! Hãy sửa chữa.");
        }

        int baseEnergy = (amountRequest >= 10) ? 12 : amountRequest;
        int expGain = (amountRequest >= 10) ? 100 : amountRequest * 10;
        int actualAmount = (amountRequest >= 10) ? 10 : amountRequest;

        boolean freeEnergy = false;
        Item toolTemplate = equippedTool.getItem();
        if (toolTemplate.getEnergySaveChance() != null && toolTemplate.getEnergySaveChance() > 0) {
            if (new Random().nextDouble() < toolTemplate.getEnergySaveChance()) {
                freeEnergy = true;
            }
        }

        if (!freeEnergy && c.getCurrentEnergy() < baseEnergy) {
            throw new RuntimeException("Thiếu năng lượng! Cần " + baseEnergy);
        }

        int durabilityCost = (amountRequest >= 10) ? 2 : 1;
        if (equippedTool.getCurrentDurability() != null) {
            equippedTool.setCurrentDurability(Math.max(0, equippedTool.getCurrentDurability() - durabilityCost));
            userItemRepo.save(equippedTool);
        }

        if (!freeEnergy) {
            c.setCurrentEnergy(c.getCurrentEnergy() - baseEnergy);
        }

        c.setGatheringRemainingAmount(c.getGatheringRemainingAmount() - actualAmount);
        addGatheringExp(c, expGain);
        addItemToInventory(c, resourceItem, actualAmount);

        if (c.getGatheringRemainingAmount() <= 0) clearGatheringState(c);
        characterRepository.save(c);

        Map<String, Object> resp = new HashMap<>();
        String msg = "Thu hoạch " + actualAmount + "x " + resourceItem.getName();
        if (freeEnergy) msg += " (Tiết kiệm sức!)";
        else msg += " (-" + baseEnergy + " Energy)";

        resp.put("message", msg);
        resp.put("remaining", c.getGatheringRemainingAmount());
        resp.put("jobExp", expGain);
        resp.put("toolDurability", equippedTool.getCurrentDurability());
        return resp;
    }

    private SlotType determineRequiredTool(String resourceCode) {
        if (resourceCode.startsWith("w_")) return SlotType.AXE;       // Wood
        if (resourceCode.startsWith("o_")) return SlotType.PICKAXE;   // Ore
        if (resourceCode.startsWith("f_")) return SlotType.FISHING_ROD; // Fish
        if (resourceCode.startsWith("s_")) return SlotType.SHOVEL;    // Sand/Soil
        return SlotType.PICKAXE;
    }

    private String getToolName(SlotType type) {
        switch (type) {
            case AXE: return "Rìu";
            case PICKAXE: return "Cúp";
            case SHOVEL: return "Xẻng";
            case FISHING_ROD: return "Cần Câu";
            default: return "Dụng cụ";
        }
    }

    private void addGatheringExp(Character c, int amount) {
        c.setGatheringExp(c.getGatheringExp() + amount);
        long req = c.getGatheringLevel() * 150L + (long)Math.pow(c.getGatheringLevel(), 2) * 10;
        if (c.getGatheringExp() >= req) {
            c.setGatheringLevel(c.getGatheringLevel() + 1);
            c.setGatheringExp(c.getGatheringExp() - req);
        }
    }

    private void clearGatheringState(Character c) {
        c.setGatheringItemId(null);
        c.setGatheringRemainingAmount(0);
        c.setGatheringExpiry(null);
    }

    private void checkExploreLevelUp(Character c) {
        long required = c.getLevel() * 100L;
        if (c.getCurrentExp() >= required) {
            c.setLevel(c.getLevel() + 1);
            c.setCurrentExp(c.getCurrentExp() - required);
            characterService.recalculateStats(c);
            c.setCurrentHp(c.getMaxHp());
            c.setMaxEnergy(c.getMaxEnergy() + 1);
        }
    }

    private void addItemToInventory(Character character, Item item, int amount) {
        int currentSlots = userItemRepo.countByCharacter_CharId(character.getCharId());
        int maxSlots = character.getUser().getInventorySlots() != null ? character.getUser().getInventorySlots() : 50;

        if (List.of("MATERIAL", "CONSUMABLE").contains(item.getType())) {
            Optional<UserItem> exist = userItemRepo.findByCharacter_CharIdAndItem_ItemId(character.getCharId(), item.getItemId())
                    .stream().filter(ui -> !ui.getIsEquipped()).findFirst();
            if (exist.isPresent()) {
                exist.get().setQuantity(exist.get().getQuantity() + amount);
                userItemRepo.save(exist.get());
                return;
            }
        }

        if (currentSlots >= maxSlots) {
            throw new RuntimeException("Kho đồ đã đầy (" + currentSlots + "/" + maxSlots + ")!");
        }

        UserItem ui = new UserItem();
        ui.setCharacter(character); ui.setItem(item);
        ui.setQuantity(amount); ui.setIsEquipped(false);
        ui.setEnhanceLevel(0);
        ui.setMythicStars(0);
        ui.setAcquiredAt(LocalDateTime.now());
        ui.setMainStatValue(BigDecimal.valueOf(item.getBaseMainStat() != null ? item.getBaseMainStat() : 0));
        ui.setMaxDurability(item.getMaxDurability() != null ? item.getMaxDurability() : 100);
        ui.setCurrentDurability(ui.getMaxDurability());

        if (List.of("WEAPON", "ARMOR", "TOOL").contains(item.getType())) {
            itemGenService.randomizeNewItem(ui);
        }
        userItemRepo.save(ui);
    }
}