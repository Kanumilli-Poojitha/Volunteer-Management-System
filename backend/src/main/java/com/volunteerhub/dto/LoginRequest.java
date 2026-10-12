package com.volunteerhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required") @Size(max = 255) String email,
        @NotBlank(message = "Password is required") @Size(max = 200) String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
