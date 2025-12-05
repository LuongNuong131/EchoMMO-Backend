package com.echommo.controller;
import com.echommo.dto.CharacterRequest;
import com.echommo.service.CharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/character")
public class CharacterController {
    @Autowired private CharacterService s;
    @GetMapping("/me") public ResponseEntity<?> me() { return ResponseEntity.ok(s.getMyCharacter()); }
    @PostMapping("/create") public ResponseEntity<?> create(@RequestBody CharacterRequest r) { try{return ResponseEntity.ok(s.createCharacter(r));}catch(Exception e){return ResponseEntity.badRequest().body(e.getMessage());}}
    @PostMapping("/rename") public ResponseEntity<?> rename(@RequestBody Map<String,String> b) { try{return ResponseEntity.ok(s.renameCharacter(b.get("name")));}catch(Exception e){return ResponseEntity.badRequest().body(e.getMessage());}}
}