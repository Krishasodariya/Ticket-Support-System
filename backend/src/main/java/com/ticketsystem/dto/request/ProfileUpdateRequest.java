package com.ticketsystem.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {
    @Email(message = "Invalid email format")
    private String email;
    private String password;
    private String profilePicture;
    private LocalDate birthDate;
}
