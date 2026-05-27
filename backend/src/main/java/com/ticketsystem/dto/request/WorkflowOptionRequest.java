package com.ticketsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowOptionRequest {
    @NotBlank(message = "Type is required")
    private String type;

    @NotBlank(message = "Name is required")
    private String name;

    private String label;
    private int sortOrder;
    private boolean active = true;
}
