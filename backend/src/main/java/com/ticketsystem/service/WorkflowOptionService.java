package com.ticketsystem.service;

import com.ticketsystem.dto.request.WorkflowOptionRequest;
import com.ticketsystem.dto.response.WorkflowOptionResponse;
import com.ticketsystem.exception.ResourceNotFoundException;
import com.ticketsystem.model.WorkflowOption;
import com.ticketsystem.repository.WorkflowOptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class WorkflowOptionService {
    private final WorkflowOptionRepository repository;

    public WorkflowOptionService(WorkflowOptionRepository repository) {
        this.repository = repository;
    }

    public List<WorkflowOptionResponse> getByType(String type, boolean onlyActive) {
        String cleanType = cleanType(type);
        return (onlyActive
                ? repository.findByTypeAndActiveTrueOrderBySortOrderAscNameAsc(cleanType)
                : repository.findByTypeOrderBySortOrderAscNameAsc(cleanType))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public WorkflowOptionResponse create(WorkflowOptionRequest request) {
        String type = cleanType(request.getType());
        String name = request.getName().trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (repository.existsByTypeIgnoreCaseAndNameIgnoreCase(type, name)) {
            throw new IllegalArgumentException("Workflow option already exists");
        }
        WorkflowOption option = WorkflowOption.builder()
                .type(type)
                .name(name)
                .label(request.getLabel() == null || request.getLabel().isBlank() ? name : request.getLabel().trim())
                .sortOrder(request.getSortOrder())
                .active(request.isActive())
                .build();
        return toResponse(repository.save(option));
    }

    @Transactional
    public WorkflowOptionResponse update(Long id, WorkflowOptionRequest request) {
        WorkflowOption option = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Workflow option not found"));
        option.setType(cleanType(request.getType()));
        option.setName(request.getName().trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        option.setLabel(request.getLabel() == null || request.getLabel().isBlank() ? option.getName() : request.getLabel().trim());
        option.setSortOrder(request.getSortOrder());
        option.setActive(request.isActive());
        return toResponse(repository.save(option));
    }

    @Transactional
    public void delete(Long id) {
        WorkflowOption option = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Workflow option not found"));
        option.setActive(false);
        repository.save(option);
    }

    private String cleanType(String type) {
        String clean = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!clean.equals("STATUS") && !clean.equals("PRIORITY")) {
            throw new IllegalArgumentException("Type must be STATUS or PRIORITY");
        }
        return clean;
    }

    private WorkflowOptionResponse toResponse(WorkflowOption option) {
        WorkflowOptionResponse response = new WorkflowOptionResponse();
        response.setId(option.getId());
        response.setType(option.getType());
        response.setName(option.getName());
        response.setLabel(option.getLabel());
        response.setSortOrder(option.getSortOrder());
        response.setActive(option.isActive());
        return response;
    }
}
