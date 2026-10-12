package com.volunteerhub.dto;

import com.volunteerhub.model.AccountStatus;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull(message = "Status is required") AccountStatus status) {
}
