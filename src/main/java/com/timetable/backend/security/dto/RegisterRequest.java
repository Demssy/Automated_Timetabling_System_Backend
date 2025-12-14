package com.timetable.backend.security.dto;

import java.time.LocalDate;

public record RegisterRequest(String email, String password, String fullName, LocalDate birthDate) {
}

