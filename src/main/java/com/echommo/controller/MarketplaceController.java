package com.echommo.controller;

import com.echommo.dto.CreateListingRequest;
import com.echommo.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // [UserItem ID là Long]
    @PostMapping("/sell")
    public ResponseEntity<?> sellToShop(@RequestBody Map<String, Object> body) {
        try {
            Long userItemId = ((Number) body.get("userItemId")).longValue();
            Integer quantity = ((Number) body.get("quantity")).intValue();
            return ResponseEntity.ok(service.sellItem(userItemId, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [UserItem ID là Long]
    @PostMapping("/create")
    public ResponseEntity<?> createListing(@RequestBody CreateListingRequest req) {
        try {
            return ResponseEntity.ok(service.createListing(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [FIX] Listing ID đổi về Integer
    @PostMapping("/buy/{id}")
    public ResponseEntity<?> buyListingByPath(@PathVariable Integer id, @RequestBody(required = false) Map<String, Integer> body) {
        try {
            int quantity = (body != null && body.containsKey("quantity")) ? body.get("quantity") : 1;
            return ResponseEntity.ok(service.buyPlayerListing(id, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [FIX] Listing ID đổi về Integer (Cast từ Object an toàn)
    @PostMapping("/buy")
    public ResponseEntity<?> buyListing(@RequestBody Map<String, Object> body) {
        try {
            Integer listingId = ((Number) body.get("listingId")).intValue(); // Cast về int
            Integer quantity = ((Number) body.get("quantity")).intValue();
            return ResponseEntity.ok(service.buyPlayerListing(listingId, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [FIX] Listing ID đổi về Integer
    @PostMapping("/cancel/{id}")
    public ResponseEntity<?> cancelListing(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.cancelListing(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}