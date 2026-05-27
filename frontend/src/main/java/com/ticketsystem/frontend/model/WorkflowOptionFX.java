package com.ticketsystem.frontend.model;

import lombok.Data;

@Data
public class WorkflowOptionFX {
    private Long id;
    private String type;
    private String name;
    private String label;
    private int sortOrder;
    private boolean active;

    @Override
    public String toString() {
        return label != null && !label.isBlank() ? label : name;
    }
}
