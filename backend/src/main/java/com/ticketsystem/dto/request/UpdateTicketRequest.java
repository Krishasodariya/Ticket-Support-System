package com.ticketsystem.dto.request;

import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;

import java.util.UUID;

public class UpdateTicketRequest {
    private String title;
    private String description;
    private TicketPriority priority;
    private Long categoryId;
    private TicketStatus status;
    private UUID assignedTo;
    private String solutionReason;
    private Integer customerRating;
    private String customerFeedback;
    private String attachmentName;
    private String attachmentPath;
    private String customPriorityLabel;
    private String customStatusLabel;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }

    public String getSolutionReason() { return solutionReason; }
    public void setSolutionReason(String solutionReason) { this.solutionReason = solutionReason; }

    public Integer getCustomerRating() { return customerRating; }
    public void setCustomerRating(Integer customerRating) { this.customerRating = customerRating; }

    public String getCustomerFeedback() { return customerFeedback; }
    public void setCustomerFeedback(String customerFeedback) { this.customerFeedback = customerFeedback; }

    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }

    public String getCustomPriorityLabel() { return customPriorityLabel; }
    public void setCustomPriorityLabel(String customPriorityLabel) { this.customPriorityLabel = customPriorityLabel; }

    public String getCustomStatusLabel() { return customStatusLabel; }
    public void setCustomStatusLabel(String customStatusLabel) { this.customStatusLabel = customStatusLabel; }
}
