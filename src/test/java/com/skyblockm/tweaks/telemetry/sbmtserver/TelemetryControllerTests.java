package com.skyblockm.tweaks.telemetry.sbmtserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyblockm.tweaks.telemetry.sbmtserver.dto.TelemetryRequestDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.TelemetryEvent;
import com.skyblockm.tweaks.telemetry.sbmtserver.repository.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TelemetryControllerTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TelemetryRepository telemetryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        telemetryRepository.deleteAll();
    }

    @Test
    void whenUploadingTelemetryWithUsername_shouldSaveUsername() throws Exception {
        TelemetryRequestDto dto = new TelemetryRequestDto(
                "Player123",
                "1.0.0",
                "1.21.1",
                "MOD_START",
                Map.of("os", "Linux")
        );

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted());

        List<TelemetryEvent> events = telemetryRepository.findAll();
        assertEquals(1, events.size());
        assertEquals("Player123", events.get(0).getUsername());
        assertEquals("MOD_START", events.get(0).getEventType());
    }

    @Test
    void whenUploadingTelemetryWithoutUsername_shouldSucceedWithNullUsername() throws Exception {
        TelemetryRequestDto dto = new TelemetryRequestDto(
                null,
                "1.0.0",
                "1.21.1",
                "MOD_START",
                Map.of()
        );

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted());

        List<TelemetryEvent> events = telemetryRepository.findAll();
        assertEquals(1, events.size());
        assertNull(events.get(0).getUsername());
    }
}
