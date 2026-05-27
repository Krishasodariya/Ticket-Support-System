package com.ticketsystem.frontend.model;

import lombok.Data;

@Data
public class CategoryFX {
    private Long id;
    private String name;
    private String description;
    
    @Override
    public String toString() {
        return name;
    }
}
