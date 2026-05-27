package com.ticketsystem.controller;

import com.ticketsystem.service.DemoDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo-data")
public class DemoDataController {
    private final DemoDataService service;

    public DemoDataController(DemoDataService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> generate() {
        return ResponseEntity.ok(Map.of("message", service.generate()));
    }
}
