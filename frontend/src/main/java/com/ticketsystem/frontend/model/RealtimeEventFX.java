package com.ticketsystem.frontend.model;

public class RealtimeEventFX {
    private String eventType;
    private String ticketId;
    private String entityId;
    private String createdAt;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isTicketOrCommentEvent() {
        return "TICKET_CREATED".equals(eventType) || "COMMENT_CREATED".equals(eventType);
    }
}
