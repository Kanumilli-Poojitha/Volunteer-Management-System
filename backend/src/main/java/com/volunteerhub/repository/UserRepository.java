package com.volunteerhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.volunteerhub.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByEmail(String email);
}