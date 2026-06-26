package com.ticketsystem.frontend.model;

import lombok.Data;

@Data
public class CategoryFX {
    private Long id;
    private String name;
    private String description;
    private String templateText;
    
    @Override
    public String toString() {
        return name;
    }
}
