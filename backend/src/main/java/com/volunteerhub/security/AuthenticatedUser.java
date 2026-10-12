package com.volunteerhub.security;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.volunteerhub.model.Role;

/** Principal placed in the SecurityContext after a JWT has been validated. Built from the database, not the token. */
public record AuthenticatedUser(Long id, String email, Role role) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }
}
