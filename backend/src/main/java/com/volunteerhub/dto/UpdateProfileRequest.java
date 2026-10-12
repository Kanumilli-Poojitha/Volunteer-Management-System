package com.volunteerhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Only the name can be changed through the self-service endpoint. Role/status/email are not accepted. */
public record UpdateProfileRequest(
        @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must be at most 100 characters") String name) {
}
