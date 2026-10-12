package com.volunteerhub.dto;

import com.volunteerhub.model.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must be at most 100 characters") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 255) String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password,
        @NotNull(message = "Role is required") Role role) {

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", role=" + role + "]"; // never print the password
    }
}
