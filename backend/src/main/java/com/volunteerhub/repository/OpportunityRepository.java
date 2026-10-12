package com.volunteerhub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.volunteerhub.model.Opportunity;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    @Query("select o from Opportunity o join fetch o.organizer order by o.eventDate asc, o.id asc")
    List<Opportunity> findAllWithOrganizer();

    @Query("select o from Opportunity o join fetch o.organizer where o.id = :id")
    Optional<Opportunity> findByIdWithOrganizer(@Param("id") Long id);
}
