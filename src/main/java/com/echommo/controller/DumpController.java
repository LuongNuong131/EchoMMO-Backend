package com.echommo.controller;

import com.echommo.dto.DumpRequest;
import com.echommo.dto.DumpResponse;
import com.echommo.service.DumpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/dump")
@RequiredArgsConstructor
public class DumpController {

    private final DumpService dumpService;

    @PostMapping("/fish")
    public ResponseEntity<DumpResponse> dumpFish(@RequestBody DumpRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        // Truyền username vào service để xử lý giao dịch an toàn
        DumpResponse response = dumpService.processDump(principal.getName(), request);
        return ResponseEntity.ok(response);
    }
}