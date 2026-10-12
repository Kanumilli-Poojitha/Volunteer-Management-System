package com.volunteerhub.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "opportunities")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Opportunity() {
    }

    public Opportunity(String title, String description, String location, LocalDate eventDate, User organizer) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.eventDate = eventDate;
        this.organizer = organizer;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public LocalDate getEventDate() { return eventDate; }
    public User getOrganizer() { return organizer; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
}
