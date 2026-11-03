package com.timetable.backend.controller;

import com.timetable.backend.domain.exception.ScheduleConflictException;
import com.timetable.backend.service.ScheduleMetadataService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScheduleMetadataControllerTest {
    @Test
    void publicationConflictIsReturnedAsHttpConflict() throws Exception {
        ScheduleMetadataService service = mock(ScheduleMetadataService.class);
        when(service.publish(1L)).thenThrow(new ScheduleConflictException("Schedule has hard violations"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ScheduleMetadataController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(patch("/api/schedules/1/publish"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Schedule has hard violations"));
    }
}
