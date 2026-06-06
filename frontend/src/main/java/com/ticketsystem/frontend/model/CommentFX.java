package com.ticketsystem.frontend.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentFX {
    private String id;
    private String content;
    private String authorUsername;
    private boolean internal;
    private LocalDateTime createdAt;
}
