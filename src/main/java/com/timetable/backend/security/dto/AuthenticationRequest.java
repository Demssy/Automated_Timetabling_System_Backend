package com.timetable.backend.security.dto;

public record AuthenticationRequest(String email, String password) {
}

