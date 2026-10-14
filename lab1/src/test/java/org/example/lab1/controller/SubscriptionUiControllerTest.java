package org.example.lab1.controller;

import org.example.lab1.dto.SubscriptionRequestDto;
import org.example.lab1.dto.SubscriptionResponseDto;
import org.example.lab1.entity.Category;
import org.example.lab1.entity.User;
import org.example.lab1.exception.GlobalUiExceptionHandler;
import org.example.lab1.service.CategoryService;
import org.example.lab1.service.SubscriptionService;
import org.example.lab1.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SubscriptionUiControllerTest {

    private MockMvc mockMvc;
    private SubscriptionService subscriptionService;
    private UserService userService;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        userService = mock(UserService.class);
        categoryService = mock(CategoryService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new SubscriptionUiController(subscriptionService, userService, categoryService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalUiExceptionHandler())
                .build();

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /subscriptions — повертає сторінку списку підписок та заповнює модель")
    void list_returnsListViewAndModel() throws Exception {
        SubscriptionResponseDto item = new SubscriptionResponseDto();
        item.setId(1L);
        item.setName("Netflix");

        when(subscriptionService.findAll()).thenReturn(List.of(item));

        mockMvc.perform(get("/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions/list"))
                .andExpect(model().attributeExists("subscriptions"));

        verify(subscriptionService).findAll();
    }

    @Test
    @DisplayName("GET /subscriptions/new — повертає форму створення з необхідними атрибутами")
    void createForm_returnsFormViewAndOptions() throws Exception {
        when(userService.findAll()).thenReturn(List.of(new User(1L, "user", "user@mail.com", "USER")));
        when(categoryService.findAll()).thenReturn(List.of(new Category(10L, "Streaming")));

        mockMvc.perform(get("/subscriptions/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions/form"))
                .andExpect(model().attributeExists("subscription", "formTitle", "formAction", "users", "categories"));

        verify(userService).findAll();
        verify(categoryService).findAll();
    }

    @Test
    @DisplayName("POST /subscriptions — перенаправляє на список після успішного створення")
    void create_validDto_redirectsToSubscriptions() throws Exception {
        OidcUser principal = mock(OidcUser.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", List.of()));

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Spotify")
                        .param("price", "9.99")
                        .param("billingDate", LocalDate.now().plusDays(5).toString())
                        .param("categoryId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscriptions"))
                .andExpect(flash().attribute("successMessage", "Підписку створено"));

        verify(subscriptionService).create(any(SubscriptionRequestDto.class), eq(principal));
    }

    @Test
    @DisplayName("POST /subscriptions — повертає форму створення при невалідній ціні")
    void create_invalidDto_returnsFormViewWithErrors() throws Exception {
        OidcUser principal = mock(OidcUser.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", List.of()));

        when(userService.findAll()).thenReturn(List.of());
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Spotify")
                        .param("price", "-5.00")
                        .param("billingDate", LocalDate.now().plusDays(5).toString())
                        .param("categoryId", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions/form"))
                .andExpect(model().attributeHasFieldErrors("subscription", "price"));

        verify(subscriptionService, never()).create(any(SubscriptionRequestDto.class), any(OidcUser.class));
    }

    @Test
    @DisplayName("GET /subscriptions/{id}/edit — повертає форму редагування заповнену даними підписки")
    void editForm_existingId_returnsFormViewWithData() throws Exception {
        OidcUser principal = mock(OidcUser.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", List.of()));

        SubscriptionResponseDto dto = new SubscriptionResponseDto();
        dto.setId(5L);
        dto.setName("YouTube Premium");
        dto.setPrice(new BigDecimal("11.99"));
        dto.setBillingDate(LocalDate.now().plusDays(15));
        dto.setUserId(1L);
        dto.setCategoryId(10L);

        when(subscriptionService.findById(5L)).thenReturn(dto);

        mockMvc.perform(get("/subscriptions/5/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions/form"))
                .andExpect(model().attributeExists("subscription", "formTitle", "formAction", "categories"));

        verify(subscriptionService).findById(5L);
    }

    @Test
    @DisplayName("POST /subscriptions/{id} — успішно оновлює підписку та перенаправляє")
    void update_validDto_redirectsToSubscriptions() throws Exception {
        OidcUser principal = mock(OidcUser.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", List.of()));

        mockMvc.perform(post("/subscriptions/5")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "YouTube Premium Family")
                        .param("price", "17.99")
                        .param("billingDate", LocalDate.now().plusDays(15).toString())
                        .param("categoryId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscriptions"))
                .andExpect(flash().attribute("successMessage", "Підписку оновлено"));

        verify(subscriptionService).update(eq(5L), any(SubscriptionRequestDto.class), eq(principal));
    }

    @Test
    @DisplayName("POST /subscriptions/{id}/delete — успішно видаляє підписку та перенаправляє")
    void delete_validId_redirectsToSubscriptions() throws Exception {
        OidcUser principal = mock(OidcUser.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", List.of()));

        mockMvc.perform(post("/subscriptions/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscriptions"))
                .andExpect(flash().attribute("successMessage", "Підписку видалено"));

        verify(subscriptionService).delete(eq(5L), eq(principal));
    }
}