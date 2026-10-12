package com.volunteerhub.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerhub.dto.UpdateProfileRequest;
import com.volunteerhub.dto.UserResponse;
import com.volunteerhub.exception.ConflictException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.model.AccountStatus;
import com.volunteerhub.model.Role;
import com.volunteerhub.model.User;
import com.volunteerhub.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ---- self-service ----

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(find(userId));
    }

    /** Only the name is editable; role, status and email can never change through this path. */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = find(userId);
        user.setName(request.name().trim());
        return UserResponse.from(userRepository.save(user));
    }

    // ---- administration (callers are restricted to ADMIN by SecurityConfig) ----

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse changeRole(Long targetId, Role newRole) {
        User target = find(targetId);
        if (target.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            guardLastActiveAdmin(target, "demote");
        }
        target.setRole(newRole);
        return UserResponse.from(userRepository.save(target));
    }

    @Transactional
    public UserResponse changeStatus(Long targetId, AccountStatus newStatus) {
        User target = find(targetId);
        if (newStatus != AccountStatus.ACTIVE) {
            guardLastActiveAdmin(target, "deactivate or suspend");
        }
        target.setStatus(newStatus);
        return UserResponse.from(userRepository.save(target));
    }

    @Transactional
    public void deleteUser(Long targetId) {
        User target = find(targetId);
        guardLastActiveAdmin(target, "delete");
        userRepository.delete(target);
    }

    /**
     * Policy: the system must always keep at least one ACTIVE admin. Removing the admin-ness of the last one
     * (demote / deactivate / suspend / delete) is rejected with 409.
     * Known limitation: two admins removing each other at the exact same instant could race; a DB-level lock would close it.
     */
    private void guardLastActiveAdmin(User target, String action) {
        boolean isActiveAdmin = target.getRole() == Role.ADMIN && target.getStatus() == AccountStatus.ACTIVE;
        if (isActiveAdmin && userRepository.countByRoleAndStatus(Role.ADMIN, AccountStatus.ACTIVE) <= 1) {
            throw new ConflictException("Cannot " + action + " the last active administrator");
        }
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
