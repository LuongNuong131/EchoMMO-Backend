package com.echommo.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@Entity
@Table(name = "wallet")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    @JsonIgnoreProperties("wallet")
    private User user;

    // [FIX] Chuyển Gold sang BigDecimal để tính toán chính xác
    @Column(precision = 20, scale = 8)
    private BigDecimal gold = BigDecimal.ZERO;

    private Integer diamonds = 0;

    @Column(name = "echo_coin", precision = 20, scale = 8)
    private BigDecimal echoCoin = BigDecimal.ZERO;

    // Nguyên liệu
    private Integer wood = 0;
    private Integer driedWood = 0;
    private Integer coldWood = 0;
    private Integer strangeWood = 0;
    private Integer coal = 0;
    private Integer copperOre = 0;
    private Integer ironOre = 0;
    private Integer platinum = 0;
    private Integer fish = 0;
    private Integer shark = 0;
    private Integer unknownMaterial = 0;

    // --- GETTER & SETTER ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // [FIX] Getter/Setter cho Gold kiểu BigDecimal
    public BigDecimal getGold() { return gold != null ? gold : BigDecimal.ZERO; }
    public void setGold(BigDecimal gold) { this.gold = gold; }

    public Integer getDiamonds() { return diamonds != null ? diamonds : 0; }
    public void setDiamonds(Integer diamonds) { this.diamonds = diamonds; }

    public BigDecimal getEchoCoin() { return echoCoin != null ? echoCoin : BigDecimal.ZERO; }
    public void setEchoCoin(BigDecimal echoCoin) { this.echoCoin = echoCoin; }

    // Getter cho nguyên liệu
    public Integer getWood() { return wood; } public void setWood(Integer v) { this.wood = v; }
    public Integer getDriedWood() { return driedWood; } public void setDriedWood(Integer v) { this.driedWood = v; }
    public Integer getColdWood() { return coldWood; } public void setColdWood(Integer v) { this.coldWood = v; }
    public Integer getStrangeWood() { return strangeWood; } public void setStrangeWood(Integer v) { this.strangeWood = v; }
    public Integer getCoal() { return coal; } public void setCoal(Integer v) { this.coal = v; }
    public Integer getCopperOre() { return copperOre; } public void setCopperOre(Integer v) { this.copperOre = v; }
    public Integer getIronOre() { return ironOre; } public void setIronOre(Integer v) { this.ironOre = v; }
    public Integer getPlatinum() { return platinum; } public void setPlatinum(Integer v) { this.platinum = v; }
    public Integer getFish() { return fish; } public void setFish(Integer v) { this.fish = v; }
    public Integer getShark() { return shark; } public void setShark(Integer v) { this.shark = v; }
    public Integer getUnknownMaterial() { return unknownMaterial; } public void setUnknownMaterial(Integer v) { this.unknownMaterial = v; }
}