package com.timetable.backend.domain.dto;

import java.util.Set;

public record AuthenticationResponse(String token, Set<String> roles) {

    public AuthenticationResponse(String token) {
        this(token, Set.of());
    }
}

