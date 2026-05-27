package com.ticketsystem.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class KnowledgeBaseResponse {
    private UUID id;
    private String title;
    private String category;
    private String solution;
    private String keywords;
    private String answerTemplate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
