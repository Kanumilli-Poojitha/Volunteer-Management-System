package com.volunteerhub.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OpportunityRequest(
        @NotBlank(message = "Title is required") @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotBlank(message = "Location is required") @Size(max = 200) String location,
        @NotNull(message = "Event date is required") LocalDate eventDate) {
}
