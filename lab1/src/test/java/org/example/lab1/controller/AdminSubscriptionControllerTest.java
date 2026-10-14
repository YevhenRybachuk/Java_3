package org.example.lab1.controller;

import jakarta.persistence.EntityNotFoundException;
import org.example.lab1.exception.GlobalApiExceptionHandler;
import org.example.lab1.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminSubscriptionControllerTest {

    private MockMvc mockMvc;
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminSubscriptionController(subscriptionService))
                .setControllerAdvice(new GlobalApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("DELETE /api/admin/subscriptions/{id} — повертає 204 No Content при успішному видаленні")
    void deleteAnySubscription_existingId_returns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/subscriptions/55"))
                .andExpect(status().isNoContent());

        verify(subscriptionService).deleteAnySubscription(55L);
    }

    @Test
    @DisplayName("DELETE /api/admin/subscriptions/{id} — повертає 404 Not Found, якщо підписку не знайдено")
    void deleteAnySubscription_nonExistingId_returns404NotFound() throws Exception {
        doThrow(new EntityNotFoundException("Підписку з id 999 не знайдено"))
                .when(subscriptionService).deleteAnySubscription(999L);

        mockMvc.perform(delete("/api/admin/subscriptions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Підписку з id 999 не знайдено"));

        verify(subscriptionService).deleteAnySubscription(999L);
    }
}