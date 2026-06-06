package com.ticketsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignTicketRequest {
    @NotNull(message = "Agent is required")
    private UUID agentId;
}
