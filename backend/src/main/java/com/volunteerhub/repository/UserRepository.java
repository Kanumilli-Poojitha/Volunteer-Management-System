package com.volunteerhub.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerhub.model.AccountStatus;
import com.volunteerhub.model.Role;
import com.volunteerhub.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRoleAndStatus(Role role, AccountStatus status);

    boolean existsByRole(Role role);
}
