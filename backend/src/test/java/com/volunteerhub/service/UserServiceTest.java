package com.volunteerhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.volunteerhub.dto.UpdateProfileRequest;
import com.volunteerhub.dto.UserResponse;
import com.volunteerhub.exception.ConflictException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.model.AccountStatus;
import com.volunteerhub.model.Role;
import com.volunteerhub.model.User;
import com.volunteerhub.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    private User user(long id, Role role) {
        User u = new User("User " + id, "u" + id + "@example.com", "hash", role);
        u.setId(id);
        return u;
    }

    @Test
    void getProfile_missingUserIsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getProfile(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_changesOnlyTheName() {
        User u = user(1, Role.VOLUNTEER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);

        UserResponse result = userService.updateProfile(1L, new UpdateProfileRequest("  New Name "));

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(u.getRole()).isEqualTo(Role.VOLUNTEER);
        assertThat(u.getEmail()).isEqualTo("u1@example.com");
        assertThat(u.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void changeRole_promotesVolunteerToOrganizer() {
        User u = user(2, Role.VOLUNTEER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);

        assertThat(userService.changeRole(2L, Role.ORGANIZER).role()).isEqualTo(Role.ORGANIZER);
    }

    @Test
    void changeRole_missingUserIsNotFound() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.changeRole(5L, Role.ORGANIZER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeRole_refusesToDemoteLastActiveAdmin() {
        User admin = user(1, Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndStatus(Role.ADMIN, AccountStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> userService.changeRole(1L, Role.VOLUNTEER)).isInstanceOf(ConflictException.class);
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRole_allowsDemotingAnAdminWhenAnotherExists() {
        User admin = user(1, Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndStatus(Role.ADMIN, AccountStatus.ACTIVE)).thenReturn(2L);
        when(userRepository.save(admin)).thenReturn(admin);

        assertThat(userService.changeRole(1L, Role.VOLUNTEER).role()).isEqualTo(Role.VOLUNTEER);
    }

    @Test
    void changeStatus_suspendsRegularUser() {
        User u = user(3, Role.VOLUNTEER);
        when(userRepository.findById(3L)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);

        assertThat(userService.changeStatus(3L, AccountStatus.SUSPENDED).status()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    void changeStatus_refusesToSuspendLastActiveAdmin() {
        User admin = user(1, Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndStatus(Role.ADMIN, AccountStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> userService.changeStatus(1L, AccountStatus.SUSPENDED))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteUser_refusesToDeleteLastActiveAdmin() {
        User admin = user(1, Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndStatus(Role.ADMIN, AccountStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser(1L)).isInstanceOf(ConflictException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_deletesRegularUser() {
        User u = user(4, Role.VOLUNTEER);
        when(userRepository.findById(4L)).thenReturn(Optional.of(u));

        userService.deleteUser(4L);

        verify(userRepository).delete(u);
    }

    @Test
    void deleteUser_missingUserIsNotFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.deleteUser(404L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
