package org.example.lab1.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminTestControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminTestController()).build();
    }

    @Test
    @DisplayName("GET /api/admin/ping — повертає 200 OK та статусний текст")
    void ping_returns200AndString() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("Only admins see this"));
    }
}