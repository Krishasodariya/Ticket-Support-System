package com.ticketsystem.frontend.model;

import lombok.Data;

@Data
public class CategoryFX {
    private Long id;
    private String name;
    private String description;
    private String templateText;
    // KAT-121: Anzahl Tickets, die diese Kategorie verwenden
    private long ticketCount;

    @Override
    public String toString() {
        return name + " (" + ticketCount + " Tickets)";
    }
}
