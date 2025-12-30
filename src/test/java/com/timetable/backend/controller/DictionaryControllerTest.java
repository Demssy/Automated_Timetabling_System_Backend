package com.timetable.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.config.SecurityConfig;
import com.timetable.backend.domain.dto.RoomDTO;
import com.timetable.backend.domain.mapper.DictionaryMapper;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import com.timetable.backend.domain.repository.RoomRepository;
import com.timetable.backend.security.JwtAuthenticationFilter;
import com.timetable.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DictionaryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DictionaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomRepository roomRepository;
    @MockitoBean
    private DanceStyleRepository danceStyleRepository;
    @MockitoBean
    private DictionaryMapper dictionaryMapper;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRoom_Success() throws Exception {
        RoomDTO roomDTO = new RoomDTO(null, "Room 1", 10, true);
        Room room = new Room("Room 1", 10, true);
        Room savedRoom = new Room("Room 1", 10, true);
        savedRoom.setId(1L);
        RoomDTO savedRoomDTO = new RoomDTO(1L, "Room 1", 10, true);

        when(dictionaryMapper.toRoom(any(RoomDTO.class))).thenReturn(room);
        when(roomRepository.save(any(Room.class))).thenReturn(savedRoom);
        when(dictionaryMapper.toRoomDTO(any(Room.class))).thenReturn(savedRoomDTO);

        mockMvc.perform(post("/api/dictionaries/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listRooms_Success() throws Exception {
        Room room = new Room("Room 1", 10, true);
        RoomDTO roomDTO = new RoomDTO(1L, "Room 1", 10, true);

        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(dictionaryMapper.toRoomDTO(room)).thenReturn(roomDTO);

        mockMvc.perform(get("/api/dictionaries/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createRoom_Forbidden() throws Exception {
        RoomDTO roomDTO = new RoomDTO(null, "Room 1", 10, true);
        mockMvc.perform(post("/api/dictionaries/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomDTO)))
                .andExpect(status().isForbidden());
    }
}

