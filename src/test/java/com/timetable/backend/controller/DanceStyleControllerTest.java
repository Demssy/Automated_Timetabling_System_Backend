package com.timetable.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.config.SecurityConfig;
import com.timetable.backend.domain.dto.DanceStyleDTO;
import com.timetable.backend.security.JwtAuthenticationFilter;
import com.timetable.backend.security.JwtService;
import com.timetable.backend.service.DanceStyleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DanceStyleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DanceStyleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DanceStyleService danceStyleService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getAllStyles_Unauthenticated_ReturnsOk() throws Exception {
        when(danceStyleService.getAllStyles()).thenReturn(List.of(
                new DanceStyleDTO(1L, "Salsa"),
                new DanceStyleDTO(2L, "Bachata")
        ));

        mockMvc.perform(get("/api/dance-styles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Salsa"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getAllStyles_AsTeacher_ReturnsOk() throws Exception {
        when(danceStyleService.getAllStyles()).thenReturn(List.of(
                new DanceStyleDTO(1L, "Salsa"),
                new DanceStyleDTO(2L, "Bachata")
        ));

        mockMvc.perform(get("/api/dance-styles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Salsa"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createStyle_AsAdmin_ReturnsCreated() throws Exception {
        var request = new DanceStyleDTO(null, "Afro House");
        var response = new DanceStyleDTO(10L, "Afro House");

        when(danceStyleService.createStyle(any(DanceStyleDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/dance-styles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Afro House"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteStyle_AsAdmin_ReturnsNoContent() throws Exception {
        doNothing().when(danceStyleService).deleteStyle(eq(5L));

        mockMvc.perform(delete("/api/dance-styles/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createStyle_AsStudent_Forbidden() throws Exception {
        var request = new DanceStyleDTO(null, "Afro House");

        mockMvc.perform(post("/api/dance-styles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}


