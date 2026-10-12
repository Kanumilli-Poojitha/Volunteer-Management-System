package com.volunteerhub.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.volunteerhub.model.AccountStatus;
import com.volunteerhub.model.User;
import com.volunteerhub.repository.UserRepository;

/**
 * Runs after the signature/expiry/issuer checks have passed. The authorities are taken from the
 * DATABASE (not from the "role" claim), so role changes, suspensions and deletions take effect on
 * the very next request instead of when the token expires.
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public JwtAuthConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long id;
        try {
            id = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Invalid token subject");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BadCredentialsException("Unknown user"));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BadCredentialsException("Account is not active");
        }
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
        return new UsernamePasswordAuthenticationToken(principal, jwt, principal.authorities());
    }
}
