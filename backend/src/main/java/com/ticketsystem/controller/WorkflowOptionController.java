package com.ticketsystem.controller;

import com.ticketsystem.dto.request.WorkflowOptionRequest;
import com.ticketsystem.dto.response.WorkflowOptionResponse;
import com.ticketsystem.service.WorkflowOptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow-options")
public class WorkflowOptionController {
    private final WorkflowOptionService service;

    public WorkflowOptionController(WorkflowOptionService service) {
        this.service = service;
    }

    @GetMapping("/{type}")
    public ResponseEntity<List<WorkflowOptionResponse>> getByType(@PathVariable String type,
                                                                  @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(service.getByType(type, activeOnly));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkflowOptionResponse> create(@Valid @RequestBody WorkflowOptionRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkflowOptionResponse> update(@PathVariable Long id, @Valid @RequestBody WorkflowOptionRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
