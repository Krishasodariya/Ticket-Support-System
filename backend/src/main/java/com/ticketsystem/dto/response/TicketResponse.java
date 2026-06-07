package com.ticketsystem.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TicketResponse {
    private UUID id;
    private String ticketNumber;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String categoryName;
    private String createdBy;
    private String assignedTo;
    private LocalDateTime dueAt;
    private boolean overdue;
    private boolean escalated;
    private String slaLabel;
    private String attachmentName;
    private Integer customerRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
