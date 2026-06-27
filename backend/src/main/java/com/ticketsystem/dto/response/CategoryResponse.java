package com.ticketsystem.dto.response;

import lombok.Data;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String templateText;
    // KAT-121: Anzahl Tickets, die diese Kategorie verwenden
    private long ticketCount;
}
