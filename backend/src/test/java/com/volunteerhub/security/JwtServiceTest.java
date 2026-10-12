package com.volunteerhub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.volunteerhub.config.JwtConfig;
import com.volunteerhub.config.JwtProperties;
import com.volunteerhub.dto.IssuedToken;
import com.volunteerhub.model.Role;
import com.volunteerhub.model.User;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";

    private final JwtProperties props = new JwtProperties(SECRET, Duration.ofMinutes(30));
    private final JwtConfig config = new JwtConfig();
    private final JwtService service = new JwtService(config.jwtEncoder(props), props);
    private final JwtDecoder decoder = config.jwtDecoder(props);

    @Test
    void issuedTokenCarriesSubjectRoleAndExpiry_butNoSecretsOrEmail() {
        User user = new User("Jane", "jane@example.com", "hash", Role.ORGANIZER);
        user.setId(42L);

        IssuedToken token = service.issue(user);
        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.<String>getClaim("role")).isEqualTo("ORGANIZER");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofMinutes(29)));
        assertThat(jwt.getClaims()).doesNotContainKeys("password", "email");
        assertThat(token.toString()).doesNotContain(token.value());
    }

    @Test
    void tamperedTokenIsRejected() {
        User user = new User("Jane", "jane@example.com", "hash", Role.VOLUNTEER);
        user.setId(1L);
        String token = service.issue(user).value();
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(Exception.class);
    }

    @Test
    void shortSecretIsRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtProperties("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(SECRET.getBytes(StandardCharsets.UTF_8).length).isGreaterThanOrEqualTo(32);
    }
}
