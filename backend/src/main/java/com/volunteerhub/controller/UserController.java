package com.volunteerhub.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.volunteerhub.dto.UpdateProfileRequest;
import com.volunteerhub.dto.UserResponse;
import com.volunteerhub.security.AuthenticatedUser;
import com.volunteerhub.service.UserService;

import jakarta.validation.Valid;

/** Self-service profile. The user id always comes from the validated token, never from the request. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.getProfile(principal.id());
    }

    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal AuthenticatedUser principal,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.id(), request);
    }
}
