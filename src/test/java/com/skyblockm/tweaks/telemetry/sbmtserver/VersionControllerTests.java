package com.skyblockm.tweaks.telemetry.sbmtserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class VersionControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void whenGettingVersion_shouldReturn200AndVersionInfo() throws Exception {
        mockMvc.perform(get("/api/v1/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.releaseUrl").isNotEmpty())
                .andExpect(jsonPath("$.stable").exists())
                .andExpect(jsonPath("$.nightly").exists());
    }

    @Test
    void whenGettingLatestVersion_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/version/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.releaseUrl").isNotEmpty());
    }

    @Test
    void whenGettingNightlyVersion_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/version/nightly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.releaseUrl").isNotEmpty());
    }
}
