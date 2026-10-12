package com.volunteerhub.dto;

import java.time.Instant;

import com.volunteerhub.model.AccountStatus;
import com.volunteerhub.model.Role;
import com.volunteerhub.model.User;

/** Safe projection of a user: no password hash. */
public record UserResponse(Long id, String name, String email, Role role, AccountStatus status, Instant createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getStatus(), u.getCreatedAt());
    }
}
