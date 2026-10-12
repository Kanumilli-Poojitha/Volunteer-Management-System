package com.volunteerhub.security;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.volunteerhub.config.JwtProperties;
import com.volunteerhub.dto.IssuedToken;
import com.volunteerhub.model.User;

@Service
public class JwtService {

    public static final String ISSUER = "volunteerhub";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock = Clock.systemUTC();

    public JwtService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    /** Issues an HS256-signed JWT: sub = user id, role, iat, exp. No password or e-mail inside. */
    public IssuedToken issue(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(properties.expiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }
}
