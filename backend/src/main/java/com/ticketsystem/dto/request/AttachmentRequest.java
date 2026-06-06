package com.ticketsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AttachmentRequest {
    @NotBlank(message = "File name is required")
    private String fileName;
    private String filePath;
}
