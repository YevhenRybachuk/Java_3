package org.example.lab1.controller;

import jakarta.persistence.EntityNotFoundException;
import org.example.lab1.config.JwtArgumentResolver;
import org.example.lab1.dto.SubscriptionRequestDto;
import org.example.lab1.dto.SubscriptionResponseDto;
import org.example.lab1.exception.GlobalApiExceptionHandler;
import org.example.lab1.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionControllerTest {

    private MockMvc mockMvc;
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new SubscriptionController(subscriptionService))
                .setCustomArgumentResolvers(new JwtArgumentResolver())
                .setControllerAdvice(new GlobalApiExceptionHandler())
                .setValidator(validator)
                .build();

        SecurityContextHolder.clearContext();
    }

    // =====================================================================
    // POST /api/subscriptions (create)
    // =====================================================================

    @Test
    @DisplayName("POST /api/subscriptions — повертає 201 Created при валідних даних")
    void create_validDto_returns201Created() throws Exception {
        Jwt jwt = mock(Jwt.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        SubscriptionResponseDto responseDto = new SubscriptionResponseDto();
        responseDto.setId(100L);
        responseDto.setName("Netflix");

        when(subscriptionService.create(any(SubscriptionRequestDto.class), eq(jwt))).thenReturn(responseDto);

        String jsonBody = """
                {
                    "name": "Netflix",
                    "price": 9.99,
                    "billingDate": "%s",
                    "categoryId": 1
                }
                """.formatted(LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("Netflix"));

        verify(subscriptionService).create(any(SubscriptionRequestDto.class), eq(jwt));
    }

    @Test
    @DisplayName("POST /api/subscriptions — повертає 400 Bad Request при невалідній ціні DTO")
    void create_invalidDto_returns400BadRequest() throws Exception {
        String jsonBody = """
                {
                    "name": "Netflix",
                    "price": -5.00,
                    "billingDate": "%s",
                    "categoryId": 1
                }
                """.formatted(LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());

        verify(subscriptionService, never()).create(any(SubscriptionRequestDto.class), any(Jwt.class));
    }

    @Test
    @DisplayName("POST /api/subscriptions — обробляє IllegalArgumentException від сервісу як 400 Bad Request")
    void create_serviceThrowsIllegalArgumentException_returns400() throws Exception {
        Jwt jwt = mock(Jwt.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        when(subscriptionService.create(any(SubscriptionRequestDto.class), eq(jwt)))
                .thenThrow(new IllegalArgumentException("Неприпустимий аргумент"));

        String jsonBody = """
                {
                    "name": "Netflix",
                    "price": 9.99,
                    "billingDate": "%s",
                    "categoryId": 1
                }
                """.formatted(LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Неприпустимий аргумент"));
    }

    // =====================================================================
    // GET /api/subscriptions/{id} (findById)
    // =====================================================================

    @Test
    @DisplayName("GET /api/subscriptions/{id} — повертає 200 OK та підписку")
    void findById_existingId_returns200() throws Exception {
        SubscriptionResponseDto responseDto = new SubscriptionResponseDto();
        responseDto.setId(100L);
        responseDto.setName("Spotify");

        when(subscriptionService.findById(100L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/subscriptions/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("Spotify"));

        verify(subscriptionService).findById(100L);
    }

    @Test
    @DisplayName("GET /api/subscriptions/{id} — обробляє EntityNotFoundException як 404 Not Found")
    void findById_nonExistingId_returns404() throws Exception {
        when(subscriptionService.findById(999L))
                .thenThrow(new EntityNotFoundException("Підписку не знайдено"));

        mockMvc.perform(get("/api/subscriptions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Підписку не знайдено"));
    }

    // =====================================================================
    // GET /api/subscriptions (findAllByUser)
    // =====================================================================

    @Test
    @DisplayName("GET /api/subscriptions?userId={id} — повертає 200 OK та список підписок")
    void findAllByUser_validUserId_returns200AndList() throws Exception {
        SubscriptionResponseDto item = new SubscriptionResponseDto();
        item.setId(1L);
        item.setName("YouTube Premium");

        when(subscriptionService.findAllByUser(10L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/subscriptions").param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("YouTube Premium"));

        verify(subscriptionService).findAllByUser(10L);
    }

    // =====================================================================
    // PUT /api/subscriptions/{id} (update)
    // =====================================================================

    @Test
    @DisplayName("PUT /api/subscriptions/{id} — повертає 200 OK при успішному оновленні")
    void update_validDto_returns200() throws Exception {
        Jwt jwt = mock(Jwt.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        SubscriptionResponseDto responseDto = new SubscriptionResponseDto();
        responseDto.setId(50L);
        responseDto.setName("Netflix Premium");

        when(subscriptionService.update(eq(50L), any(SubscriptionRequestDto.class), eq(jwt))).thenReturn(responseDto);

        String jsonBody = """
                {
                    "name": "Netflix Premium",
                    "price": 14.99,
                    "billingDate": "%s",
                    "categoryId": 1
                }
                """.formatted(LocalDate.now().plusDays(10));

        mockMvc.perform(put("/api/subscriptions/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.name").value("Netflix Premium"));

        verify(subscriptionService).update(eq(50L), any(SubscriptionRequestDto.class), eq(jwt));
    }

    @Test
    @DisplayName("PUT /api/subscriptions/{id} — повертає 400 Bad Request при невалідній назві DTO")
    void update_invalidDto_returns400() throws Exception {
        String jsonBody = """
                {
                    "name": "",
                    "price": 14.99,
                    "billingDate": "%s",
                    "categoryId": 1
                }
                """.formatted(LocalDate.now().plusDays(10));

        mockMvc.perform(put("/api/subscriptions/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());

        verify(subscriptionService, never()).update(any(Long.class), any(SubscriptionRequestDto.class), any(Jwt.class));
    }

    @Test
    @DisplayName("PUT /api/subscriptions/{id} — обробляє AccessDeniedException від сервісу як 403 Forbidden")
    void update_accessDenied_returns403() throws Exception {
        Jwt jwt = mock(Jwt.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        when(subscriptionService.update(eq(50L), any(SubscriptionRequestDto.class), eq(jwt)))
                .thenThrow(new AccessDeniedException("Заборонено редагувати чужу підписку"));

        String jsonBody = """
                {
                    "name": "Netflix Premium",
                    "price": 14.99,
                    "billingDate": "%s",
                    "categoryId": 1
                }
                """.formatted(LocalDate.now().plusDays(10));

        mockMvc.perform(put("/api/subscriptions/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Заборонено редагувати чужу підписку"));
    }

    // =====================================================================
    // DELETE /api/subscriptions/{id} (delete)
    // =====================================================================

    @Test
    @DisplayName("DELETE /api/subscriptions/{id} — повертає 204 No Content")
    void delete_validId_returns204() throws Exception {
        Jwt jwt = mock(Jwt.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        mockMvc.perform(delete("/api/subscriptions/100"))
                .andExpect(status().isNoContent());

        verify(subscriptionService).delete(100L, jwt);
    }

    @Test
    @DisplayName("DELETE /api/subscriptions/{id}/force — повертає 204 No Content для примусового видалення")
    void forceDelete_validId_returns204() throws Exception {
        mockMvc.perform(delete("/api/subscriptions/100/force"))
                .andExpect(status().isNoContent());

        verify(subscriptionService).deleteAnySubscription(100L);
    }
}