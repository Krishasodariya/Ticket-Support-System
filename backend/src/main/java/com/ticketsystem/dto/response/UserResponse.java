package com.ticketsystem.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String role;
    private boolean isActive;
    private String profilePicture;
    private LocalDate birthDate;
    private LocalDateTime createdAt;
}
