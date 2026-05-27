package com.ticketsystem.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FeedbackRequest {
    @Min(1)
    @Max(5)
    private int rating;
    private String feedback;
}
