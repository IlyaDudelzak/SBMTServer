package com.skyblockm.tweaks.telemetry.sbmtserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class DashboardControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void whenAccessingRoot_shouldRedirectToDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void whenAccessingDashboard_shouldReturn200WithoutAuth() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void whenAccessingDashboardApiStats_shouldReturn200WithoutAuth() throws Exception {
        mockMvc.perform(get("/dashboard/api/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessingDashboardApiCaptchas_shouldReturn200WithoutAuth() throws Exception {
        mockMvc.perform(get("/dashboard/api/captchas"))
                .andExpect(status().isOk());
    }
}
