package com.volunteerhub.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerhub.dto.OpportunityRequest;
import com.volunteerhub.dto.OpportunityResponse;
import com.volunteerhub.exception.ForbiddenOperationException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.model.Opportunity;
import com.volunteerhub.model.Role;
import com.volunteerhub.model.User;
import com.volunteerhub.repository.OpportunityRepository;
import com.volunteerhub.repository.UserRepository;
import com.volunteerhub.security.AuthenticatedUser;

@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;

    public OpportunityService(OpportunityRepository opportunityRepository, UserRepository userRepository) {
        this.opportunityRepository = opportunityRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> list() {
        return opportunityRepository.findAllWithOrganizer().stream().map(OpportunityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OpportunityResponse get(Long id) {
        return OpportunityResponse.from(find(id));
    }

    @Transactional
    public OpportunityResponse create(AuthenticatedUser actor, OpportunityRequest request) {
        User organizer = userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Opportunity saved = opportunityRepository.save(new Opportunity(request.title().trim(),
                request.description(), request.location().trim(), request.eventDate(), organizer));
        return OpportunityResponse.from(saved);
    }

    /** Ownership rule: only the organizer who created an opportunity may change it. */
    @Transactional
    public OpportunityResponse update(AuthenticatedUser actor, Long id, OpportunityRequest request) {
        Opportunity o = find(id);
        requireOwner(actor, o);
        o.setTitle(request.title().trim());
        o.setDescription(request.description());
        o.setLocation(request.location().trim());
        o.setEventDate(request.eventDate());
        return OpportunityResponse.from(opportunityRepository.save(o));
    }

    /** The owner may delete; an ADMIN may also delete (moderation). */
    @Transactional
    public void delete(AuthenticatedUser actor, Long id) {
        Opportunity o = find(id);
        if (actor.role() != Role.ADMIN) {
            requireOwner(actor, o);
        }
        opportunityRepository.delete(o);
    }

    private void requireOwner(AuthenticatedUser actor, Opportunity o) {
        if (!o.getOrganizer().getId().equals(actor.id())) {
            throw new ForbiddenOperationException("You can only modify your own opportunities");
        }
    }

    private Opportunity find(Long id) {
        return opportunityRepository.findByIdWithOrganizer(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }
}
