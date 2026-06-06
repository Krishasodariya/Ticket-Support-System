package com.ticketsystem.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TicketDetailResponse {
    private UUID id;
    private String ticketNumber;
    private String title;
    private String description;
    private String status;
    private String priority;
    private CategoryResponse category;
    private UserResponse createdByUser;
    private UserResponse assignedToUser;
    private String createdBy;
    private String assignedTo;
    private LocalDateTime dueAt;
    private boolean overdue;
    private boolean escalated;
    private String slaLabel;
    private String solutionReason;
    private Integer customerRating;
    private String customerFeedback;
    private String attachmentName;
    private String attachmentPath;
    private String customPriorityLabel;
    private String customStatusLabel;
    private Long resolutionTimeHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
