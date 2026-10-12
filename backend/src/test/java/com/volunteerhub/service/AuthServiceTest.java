package com.volunteerhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private RegisterRequest registration(Role role) {
        return new RegisterRequest("  Jane Doe ", "Jane@Example.COM ", "Sup3rSecret!", role);
    }

    @Test
    void register_hashesPasswordNormalizesEmailAndReturnsSafeProfile() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Sup3rSecret!")).thenReturn("$2a$10$hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = authService.register(registration(Role.VOLUNTEER));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed").isNotEqualTo("Sup3rSecret!");
        assertThat(saved.getValue().getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getValue().getName()).isEqualTo("Jane Doe");
        assertThat(response.role()).isEqualTo(Role.VOLUNTEER);
        assertThat(response.toString()).doesNotContain("Sup3rSecret!").doesNotContain("$2a$");
    }

    @Test
    void register_allowsOrganizer() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Sup3rSecret!")).thenReturn("hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(authService.register(registration(Role.ORGANIZER)).role()).isEqualTo(Role.ORGANIZER);
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registration(Role.VOLUNTEER)))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_mapsUniqueIndexRaceToConflict() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Sup3rSecret!")).thenReturn("hash");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> authService.register(registration(Role.VOLUNTEER)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void register_rejectsSelfRegistrationAsAdmin() {
        assertThatThrownBy(() -> authService.register(registration(Role.ADMIN)))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        User user = new User("Jane", "jane@example.com", "hash", Role.VOLUNTEER);
        user.setId(7L);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(jwtService.issue(user)).thenReturn(new IssuedToken("jwt-value", Instant.now().plusSeconds(3600)));
        when(jwtService.expiresInSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest(" Jane@example.com", "Sup3rSecret!"));

        verify(authenticationManager).authenticate(any());
        assertThat(response.token()).isEqualTo("jwt-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("jane@example.com");
    }

    @Test
    void login_invalidPasswordGivesGenericError() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_unknownEmailIsIndistinguishableFromWrongPassword() {
        // DaoAuthenticationProvider reports unknown users as BadCredentialsException as well.
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever1")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
        verify(jwtService, never()).issue(any());
    }

    @Test
    void login_suspendedAccountGetsSameGenericError() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "Sup3rSecret!")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
