package com.ticketsystem.dto.response;

import java.time.Instant;
import java.util.UUID;

public record RealtimeEventResponse(
        String eventType,
        UUID ticketId,
        UUID entityId,
        Instant createdAt
) {
}
