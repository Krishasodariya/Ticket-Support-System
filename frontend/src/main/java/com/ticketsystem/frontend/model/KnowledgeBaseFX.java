package com.ticketsystem.frontend.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseFX {
    private String id;
    private String title;
    private String category;
    private String solution;
    private String keywords;
    private String answerTemplate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return title + " [" + category + "]";
    }
}
