package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "resource_unavailability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ResourceUnavailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Изменено с Teacher на AbstractUser для поддержки студентов
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AbstractUser user;

    // Заменили Timeslot на конкретную дату и промежуток времени
    @Column(name = "unavailability_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "reason")
    private String reason;
}
