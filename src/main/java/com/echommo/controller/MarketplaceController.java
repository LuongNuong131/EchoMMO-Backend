package com.echommo.controller;

import com.echommo.dto.CreateListingRequest;
import com.echommo.entity.MarketListing;
import com.echommo.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketplaceController {

    @Autowired private MarketplaceService service;

    @GetMapping("/shop-items")
    public ResponseEntity<?> getShopItems() {
        return ResponseEntity.ok(service.getShopItems());
    }

    @GetMapping("/listings")
    public ResponseEntity<?> getListings() {
        return ResponseEntity.ok(service.getPlayerListings());
    }

    @GetMapping("/my-listings")
    public ResponseEntity<?> getMyListings() {
        return ResponseEntity.ok(service.getMyListings());
    }

    @PostMapping("/buy-shop")
    public ResponseEntity<?> buyFromShop(@RequestBody Map<String, Integer> body) {
        try {
            return ResponseEntity.ok(service.buyItem(body.get("itemId"), body.get("quantity")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<?> sellToShop(@RequestBody Map<String, Object> body) { // [FIX] Map<String, Object> để handle Long
        try {
            // [FIX] Ép kiểu an toàn từ Integer/Long trong JSON sang Long
            Long userItemId = ((Number) body.get("userItemId")).longValue();
            Integer quantity = ((Number) body.get("quantity")).intValue();
            return ResponseEntity.ok(service.sellItem(userItemId, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createListing(@RequestBody CreateListingRequest req) {
        try {
            return ResponseEntity.ok(service.createListing(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buyListing(@RequestBody Map<String, Integer> body) {
        try {
            return ResponseEntity.ok(service.buyPlayerListing(body.get("listingId"), body.get("quantity")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/cancel/{id}")
    public ResponseEntity<?> cancelListing(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.cancelListing(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}