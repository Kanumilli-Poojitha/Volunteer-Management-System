package com.volunteerhub.dto;

import java.time.Instant;

public record IssuedToken(String value, Instant expiresAt) {

    @Override
    public String toString() {
        return "IssuedToken[expiresAt=" + expiresAt + "]"; // never print the token
    }
}
