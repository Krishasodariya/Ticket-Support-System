package com.ticketsystem.service;

import com.ticketsystem.dto.response.RealtimeEventResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RealtimeEventService {

    public static final String TICKET_CREATED = "TICKET_CREATED";
    public static final String COMMENT_CREATED = "COMMENT_CREATED";

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishTicketCreated(UUID ticketId) {
        publish(TICKET_CREATED, ticketId, ticketId);
    }

    public void publishCommentCreated(UUID ticketId, UUID commentId) {
        publish(COMMENT_CREATED, ticketId, commentId);
    }

    private void publish(String eventType, UUID ticketId, UUID entityId) {
        messagingTemplate.convertAndSend(
                "/topic/realtime",
                new RealtimeEventResponse(eventType, ticketId, entityId, Instant.now())
        );
    }
}
