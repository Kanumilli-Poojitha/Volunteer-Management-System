package com.volunteerhub.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerhub.dto.AuthResponse;
import com.volunteerhub.dto.IssuedToken;
import com.volunteerhub.dto.LoginRequest;
import com.volunteerhub.dto.RegisterRequest;
import com.volunteerhub.dto.UserResponse;
import com.volunteerhub.exception.ConflictException;
import com.volunteerhub.exception.ForbiddenOperationException;
import com.volunteerhub.model.Role;
import com.volunteerhub.model.User;
import com.volunteerhub.repository.UserRepository;
import com.volunteerhub.security.JwtService;

@Service
public class AuthService {

    static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ForbiddenOperationException("Self-registration as ADMIN is not allowed");
        }
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), request.role());
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent registrations passed the exists-check; the unique index decides.
            throw new ConflictException("An account with this email already exists");
        }
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());
        try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException e) {
            // Unknown email, wrong password and disabled accounts all look identical to the client.
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));
        IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.value(), "Bearer", jwtService.expiresInSeconds(), UserResponse.from(user));
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
