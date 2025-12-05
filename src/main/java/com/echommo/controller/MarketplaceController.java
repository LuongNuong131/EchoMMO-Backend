package com.echommo.controller;

import com.echommo.dto.CreateListingRequest;
import com.echommo.entity.Item;
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
    @Autowired private MarketplaceService s;

    @GetMapping("/shop-items") public ResponseEntity<List<Item>> shop() { return ResponseEntity.ok(s.getShopItems()); }
    @GetMapping("/listings") public ResponseEntity<List<MarketListing>> p2p() { return ResponseEntity.ok(s.getPlayerListings()); }
    @GetMapping("/my-listings") public ResponseEntity<List<MarketListing>> mine() { return ResponseEntity.ok(s.getMyListings()); }

    @PostMapping("/buy/{id}") public ResponseEntity<?> buyShop(@PathVariable Integer id, @RequestBody Map<String, Integer> b) {
        try { return ResponseEntity.ok(s.buyItem(id, b.getOrDefault("quantity", 1))); } catch(Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
    @PostMapping("/sell/{id}") public ResponseEntity<?> sellNPC(@PathVariable Integer id, @RequestBody Map<String, Integer> b) {
        try { return ResponseEntity.ok(s.sellItem(id, b.getOrDefault("quantity", 1))); } catch(Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
    @PostMapping("/create-listing") public ResponseEntity<?> create(@RequestBody CreateListingRequest r) {
        try { return ResponseEntity.ok(s.createListing(r)); } catch(Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
    @PostMapping("/cancel-listing/{id}") public ResponseEntity<?> cancel(@PathVariable Integer id) {
        try { return ResponseEntity.ok(s.cancelListing(id)); } catch(Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
    @PostMapping("/buy-listing/{id}") public ResponseEntity<?> buyP2P(@PathVariable Integer id, @RequestBody Map<String, Integer> b) {
        try { return ResponseEntity.ok(s.buyPlayerListing(id, b.getOrDefault("quantity", 1))); } catch(Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
}