package com.ticketsystem.frontend.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketFX {
    private String id;
    private String ticketNumber;
    private String title;
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
    
    // Detail fields
    private String description;
    private CategoryFX category;
    private UserFX createdByUser;
    private UserFX assignedToUser;
    private String solutionReason;
    private String customerFeedback;
    private String attachmentPath;
    private String customPriorityLabel;
    private String customStatusLabel;
    private Long resolutionTimeHours;
}

