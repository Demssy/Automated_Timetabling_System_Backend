package com.timetable.backend.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "schedule_publication_lock")
public class SchedulePublicationLock {
    @Id
    private Long id;
}
