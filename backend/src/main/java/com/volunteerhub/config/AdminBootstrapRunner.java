package com.volunteerhub.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerhub.model.AccountStatus;
import com.volunteerhub.model.Role;
import com.volunteerhub.model.User;
import com.volunteerhub.repository.UserRepository;

/**
 * Trusted bootstrap for the first ADMIN. Runs only when BOTH BOOTSTRAP_ADMIN_EMAIL and
 * BOOTSTRAP_ADMIN_PASSWORD are set and no ADMIN exists yet. Public registration can never create admins.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrapRunner(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                @Value("${app.bootstrap-admin.email:}") String email,
                                @Value("${app.bootstrap-admin.password:}") String password,
                                @Value("${app.bootstrap-admin.name:Administrator}") String name) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin bootstrap skipped: an ADMIN account already exists");
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        User admin = userRepository.findByEmail(normalized).orElse(null);
        if (admin == null) {
            admin = new User(name.trim(), normalized, passwordEncoder.encode(password), Role.ADMIN);
        } else {
            // Operator-controlled promotion of an existing (e.g. migrated legacy) account; password is reset too.
            admin.setRole(Role.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode(password));
        }
        admin.setStatus(AccountStatus.ACTIVE);
        userRepository.save(admin);
        log.info("Bootstrap admin account created/promoted (id={})", admin.getId());
    }
}
