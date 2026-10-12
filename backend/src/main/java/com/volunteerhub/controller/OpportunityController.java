package com.volunteerhub.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.volunteerhub.dto.OpportunityRequest;
import com.volunteerhub.dto.OpportunityResponse;
import com.volunteerhub.security.AuthenticatedUser;
import com.volunteerhub.service.OpportunityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private final OpportunityService service;

    public OpportunityController(OpportunityService service) {
        this.service = service;
    }

    @GetMapping
    public List<OpportunityResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public OpportunityResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<OpportunityResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @Valid @RequestBody OpportunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(principal, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public OpportunityResponse update(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @PathVariable Long id, @Valid @RequestBody OpportunityRequest request) {
        return service.update(principal, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        service.delete(principal, id);
        return ResponseEntity.noContent().build();
    }
}
