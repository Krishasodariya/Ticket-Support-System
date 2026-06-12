package com.ticketsystem.frontend.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
    private Integer feedbackRating;
    private String customerFeedback;
    @Setter(lombok.AccessLevel.NONE) // manueller Setter unten
    private String resolvedAt;

    // Jackson: LocalDateTime kommt als int-Array [year,month,day,hour,min,...] → in String umwandeln
    @com.fasterxml.jackson.annotation.JsonProperty("resolvedAt")
    public void setResolvedAt(Object value) {
        if (value == null) { this.resolvedAt = null; return; }
        if (value instanceof String s) { this.resolvedAt = s.length() >= 10 ? s.substring(0, 10) : s; return; }
        if (value instanceof java.util.List<?> list && list.size() >= 3) {
            this.resolvedAt = String.format("%04d-%02d-%02d", list.get(0), list.get(1), list.get(2));
        }
    }

    public String getResolvedAt() { return resolvedAt; }
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Detail fields
    private String description;
    private CategoryFX category;
    private UserFX createdByUser;
    private UserFX assignedToUser;
    private String solutionReason;
    private String attachmentPath;
    private String customPriorityLabel;
    private String customStatusLabel;
    private Long resolutionTimeHours;
}