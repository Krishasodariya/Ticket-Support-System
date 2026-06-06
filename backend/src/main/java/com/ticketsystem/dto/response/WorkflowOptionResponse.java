package com.ticketsystem.dto.response;

import lombok.Data;

@Data
public class WorkflowOptionResponse {
    private Long id;
    private String type;
    private String name;
    private String label;
    private int sortOrder;
    private boolean active;
}
