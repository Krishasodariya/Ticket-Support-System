package com.ticketsystem.mapper;

import com.ticketsystem.dto.response.TicketDetailResponse;
import com.ticketsystem.dto.response.TicketResponse;
import com.ticketsystem.model.Ticket;
import com.ticketsystem.model.enums.TicketStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class TicketMapper {
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    public TicketMapper(UserMapper userMapper, CategoryMapper categoryMapper) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
    }

    public TicketResponse toResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setTicketNumber(ticket.getTicketNumber());
        response.setTitle(ticket.getTitle());
        response.setDescription(ticket.getDescription());
        response.setStatus(ticket.getStatus().name());
        response.setPriority(ticket.getPriority().name());
        if (ticket.getCategory() != null) {
            response.setCategoryName(ticket.getCategory().getName());
        }
        response.setCreatedBy(ticket.getCreatedBy().getUsername());
        if (ticket.getAssignedTo() != null) {
            response.setAssignedTo(ticket.getAssignedTo().getUsername());
        }
        response.setDueAt(ticket.getDueAt());
        response.setOverdue(isOverdue(ticket));
        response.setEscalated(ticket.isEscalated());
        response.setSlaLabel(slaLabel(ticket));
        response.setAttachmentName(ticket.getAttachmentName());
        response.setCustomerRating(ticket.getCustomerRating());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        return response;
    }

    public TicketDetailResponse toDetailResponse(Ticket ticket) {
        TicketDetailResponse response = new TicketDetailResponse();
        response.setId(ticket.getId());
        response.setTicketNumber(ticket.getTicketNumber());
        response.setTitle(ticket.getTitle());
        response.setDescription(ticket.getDescription());
        response.setStatus(ticket.getStatus().name());
        response.setPriority(ticket.getPriority().name());
        
        if (ticket.getCategory() != null) {
            response.setCategory(categoryMapper.toResponse(ticket.getCategory()));
        }
        response.setCreatedByUser(userMapper.toResponse(ticket.getCreatedBy()));
        response.setCreatedBy(ticket.getCreatedBy().getUsername());
        if (ticket.getAssignedTo() != null) {
            response.setAssignedToUser(userMapper.toResponse(ticket.getAssignedTo()));
            response.setAssignedTo(ticket.getAssignedTo().getUsername());
        }
        response.setDueAt(ticket.getDueAt());
        response.setOverdue(isOverdue(ticket));
        response.setEscalated(ticket.isEscalated());
        response.setSlaLabel(slaLabel(ticket));
        response.setSolutionReason(ticket.getSolutionReason());
        response.setCustomerRating(ticket.getCustomerRating());
        response.setCustomerFeedback(ticket.getCustomerFeedback());
        response.setAttachmentName(ticket.getAttachmentName());
        response.setAttachmentPath(ticket.getAttachmentPath());
        response.setCustomPriorityLabel(ticket.getCustomPriorityLabel());
        response.setCustomStatusLabel(ticket.getCustomStatusLabel());
        response.setResolutionTimeHours(resolutionHours(ticket));
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        response.setResolvedAt(ticket.getResolvedAt());
        return response;
    }
    private boolean isOverdue(Ticket ticket) {
        return ticket.getDueAt() != null
                && LocalDateTime.now().isAfter(ticket.getDueAt())
                && ticket.getStatus() != TicketStatus.RESOLVED
                && ticket.getStatus() != TicketStatus.CLOSED;
    }

    private String slaLabel(Ticket ticket) {
        if (ticket.getDueAt() == null) return "Keine SLA";
        if (isOverdue(ticket)) return "Überfällig";
        long hours = Duration.between(LocalDateTime.now(), ticket.getDueAt()).toHours();
        if (hours <= 24) return "Fällig in " + Math.max(hours, 0) + "h";
        return "Fällig in " + Duration.between(LocalDateTime.now(), ticket.getDueAt()).toDays() + " Tagen";
    }

    private Long resolutionHours(Ticket ticket) {
        if (ticket.getResolvedAt() == null || ticket.getCreatedAt() == null) return null;
        return Duration.between(ticket.getCreatedAt(), ticket.getResolvedAt()).toHours();
    }

}
