package com.ticketsystem.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CommentResponse {
    private UUID id;
    private String content;
    private String authorUsername;
    private boolean isInternal;
    private LocalDateTime createdAt;
}
