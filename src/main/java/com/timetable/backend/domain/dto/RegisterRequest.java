package com.timetable.backend.domain.dto;

import java.time.LocalDate;

public record RegisterRequest(String email, String password, String fullName, LocalDate birthDate) {
}

