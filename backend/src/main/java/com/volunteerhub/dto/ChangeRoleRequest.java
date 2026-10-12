package com.volunteerhub.dto;

import com.volunteerhub.model.Role;

import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull(message = "Role is required") Role role) {
}
