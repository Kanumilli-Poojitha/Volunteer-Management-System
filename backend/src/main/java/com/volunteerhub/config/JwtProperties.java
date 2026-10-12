package com.volunteerhub.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** app.jwt.* - the secret comes from the JWT_SECRET environment variable and must be >= 32 bytes. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, @DefaultValue("1h") Duration expiration) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret (env JWT_SECRET) must be set and at least 32 bytes long");
        }
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalStateException("app.jwt.expiration must be a positive duration, e.g. 1h");
        }
    }

    @Override
    public String toString() {
        return "JwtProperties[expiration=" + expiration + "]"; // never print the secret
    }
}
