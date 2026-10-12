package com.volunteerhub.dto;

import java.time.LocalDate;

import com.volunteerhub.model.Opportunity;

public record OpportunityResponse(Long id, String title, String description, String location,
                                  LocalDate eventDate, Long organizerId, String organizerName) {

    public static OpportunityResponse from(Opportunity o) {
        return new OpportunityResponse(o.getId(), o.getTitle(), o.getDescription(), o.getLocation(),
                o.getEventDate(), o.getOrganizer().getId(), o.getOrganizer().getName());
    }
}
