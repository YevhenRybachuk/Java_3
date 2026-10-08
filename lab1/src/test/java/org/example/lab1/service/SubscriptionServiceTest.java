package org.example.lab1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.lab1.dto.SubscriptionRequestDto;
import org.example.lab1.dto.SubscriptionResponseDto;
import org.example.lab1.entity.Category;
import org.example.lab1.entity.Subscription;
import org.example.lab1.entity.User;
import org.example.lab1.mapper.SubscriptionMapper;
import org.example.lab1.repository.CategoryRepository;
import org.example.lab1.repository.SubscriptionRepository;
import org.example.lab1.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.junit.jupiter.api.DisplayName;

/**
 * Unit-тести для {@link SubscriptionService}.
 * <p>
 * Без Spring-контексту: залежності замокано через Mockito, час фіксований (детермінований),
 * БД / мережа / файлова система не використовуються.
 * <p>
 * Примітка: {@code @PreAuthorize("hasRole('ADMIN')")} на {@code deleteAnySubscription} працює
 * лише через Spring AOP-проксі, тому в чистому unit-тесті не перевіряється - тут тестується
 * бізнес-логіка методу. Перевірка ролі демонструється окремо (Postman, AC7 лаби №1).
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final LocalDate BILLING_DATE = LocalDate.of(2030, 1, 15);
    private static final Instant ISSUED_AT = Instant.parse("2030-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2030-01-01T01:00:00Z");

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionService subscriptionService;

    // =====================================================================
    // create(dto, OidcUser)
    // =====================================================================

    @Test
    @DisplayName("Створення (OIDC): звичайний користувач створює підписку для себе — успішно")
    void create_oidcRegularUserForSelf_savesSubscriptionOwnedByCurrentUser() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        Category streaming = category(10L, "Streaming");
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(1L, 10L);
        Subscription mapped = new Subscription();
        SubscriptionResponseDto expected = responseDto(100L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        stubCreatePipeline(dto, streaming, mapped, expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.create(dto, principal);

        // Assert
        assertSame(expected, result);
        Subscription saved = capturedSavedSubscription();
        assertSame(currentUser, saved.getUser());
        assertSame(streaming, saved.getCategory());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Створення (OIDC): якщо userId не вказано в DTO, автоматично призначається поточний користувач")
    void create_oidcRegularUserWithoutUserIdInRequest_assignsCurrentUser() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        Category streaming = category(10L, "Streaming");
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(null, 10L);
        Subscription mapped = new Subscription();
        SubscriptionResponseDto expected = responseDto(100L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        stubCreatePipeline(dto, streaming, mapped, expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.create(dto, principal);

        // Assert
        assertSame(expected, result);
        assertSame(currentUser, capturedSavedSubscription().getUser());
    }

    @Test
    @DisplayName("Створення (OIDC): звичайний користувач намагається створити підписку для чужого ID — AccessDeniedException")
    void create_oidcRegularUserForAnotherUser_throwsAccessDenied() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(2L, 10L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> subscriptionService.create(dto, principal));
        verifyNoInteractions(subscriptionRepository, categoryRepository);
        verify(subscriptionMapper, never()).toEntity(any());
    }

    @Test
    @DisplayName("Створення (OIDC): адмін може створити підписку для будь-якого іншого користувача")
    void create_oidcAdminForAnotherUser_assignsRequestedUser() {
        // Arrange
        User admin = user(100L, "admin", "ADMIN");
        User target = user(2L, "evgen", "USER");
        Category streaming = category(10L, "Streaming");
        OidcUser principal = oidcUser("admin", "sub-100", "admin@mail.com", "ROLE_ADMIN");
        SubscriptionRequestDto dto = requestDto(2L, 10L);
        Subscription mapped = new Subscription();
        SubscriptionResponseDto expected = responseDto(100L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        stubCreatePipeline(dto, streaming, mapped, expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.create(dto, principal);

        // Assert
        assertSame(expected, result);
        assertSame(target, capturedSavedSubscription().getUser());
    }

    @Test
    @DisplayName("Створення (OIDC): роль 'ADMIN' без префікса 'ROLE_' розпізнається як адмін")
    void create_oidcAuthorityWithoutRolePrefix_treatedAsAdmin() {
        // Arrange
        User admin = user(100L, "admin", "ADMIN");
        User target = user(2L, "evgen", "USER");
        Category streaming = category(10L, "Streaming");
        OidcUser principal = oidcUser("admin", "sub-100", "admin@mail.com", "ADMIN");
        SubscriptionRequestDto dto = requestDto(2L, 10L);
        Subscription mapped = new Subscription();
        SubscriptionResponseDto expected = responseDto(100L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        stubCreatePipeline(dto, streaming, mapped, expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.create(dto, principal);

        // Assert
        assertSame(expected, result);
        assertSame(target, capturedSavedSubscription().getUser());
    }

    @Test
    @DisplayName("Створення (OIDC): адмін вказує неіснуючий ID користувача — EntityNotFoundException")
    void create_oidcAdminForUnknownUser_throwsEntityNotFound() {
        // Arrange
        User admin = user(100L, "admin", "ADMIN");
        OidcUser principal = oidcUser("admin", "sub-100", "admin@mail.com", "ROLE_ADMIN");
        SubscriptionRequestDto dto = requestDto(2L, 10L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(subscriptionMapper.toEntity(dto)).thenReturn(new Subscription());
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> subscriptionService.create(dto, principal));
        assertEquals("User with id 2 was not found", ex.getMessage());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Створення (OIDC): вказано неіснуючу категорію — EntityNotFoundException")
    void create_oidcUnknownCategory_throwsEntityNotFound() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(1L, 99L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        when(subscriptionMapper.toEntity(dto)).thenReturn(new Subscription());
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> subscriptionService.create(dto, principal));
        assertEquals("Category with id 99 was not found", ex.getMessage());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Створення (OIDC): перший вхід користувача — автоматичне створення запису User у БД")
    void create_oidcFirstLogin_createsUserFromPrincipalClaims() {
        // Arrange
        Category streaming = category(10L, "Streaming");
        OidcUser principal = oidcUser("newbie", "sub-5", "newbie@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(null, 10L);
        Subscription mapped = new Subscription();
        SubscriptionResponseDto expected = responseDto(100L);
        when(userRepository.findByUsername("newbie")).thenReturn(Optional.empty());
        stubUserSaveAssigningId(5L);
        stubCreatePipeline(dto, streaming, mapped, expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.create(dto, principal);

        // Assert
        assertSame(expected, result);
        User createdUser = capturedSavedUser();
        assertEquals("newbie", createdUser.getUsername());
        assertEquals("newbie@mail.com", createdUser.getEmail());
        assertEquals("USER", createdUser.getRole());
        assertSame(createdUser, capturedSavedSubscription().getUser());
    }

    @Test
    @DisplayName("Створення (OIDC): перший вхід без email — використання резервної пошти (@example.com)")
    void create_oidcFirstLoginWithoutEmail_usesFallbackEmail() {
        // Arrange
        Category streaming = category(10L, "Streaming");
        OidcUser principal = oidcUser("newbie", "sub-5", null, "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(null, 10L);
        when(userRepository.findByUsername("newbie")).thenReturn(Optional.empty());
        stubUserSaveAssigningId(5L);
        stubCreatePipeline(dto, streaming, new Subscription(), responseDto(100L));

        // Act
        subscriptionService.create(dto, principal);

        // Assert
        assertEquals("newbie@example.com", capturedSavedUser().getEmail());
    }

    @Test
    @DisplayName("Створення (OIDC): перший вхід із порожнім email — використання резервної пошти")
    void create_oidcFirstLoginWithBlankEmail_usesFallbackEmail() {
        // Arrange
        Category streaming = category(10L, "Streaming");
        OidcUser principal = oidcUser("newbie", "sub-5", "   ", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(null, 10L);
        when(userRepository.findByUsername("newbie")).thenReturn(Optional.empty());
        stubUserSaveAssigningId(5L);
        stubCreatePipeline(dto, streaming, new Subscription(), responseDto(100L));

        // Act
        subscriptionService.create(dto, principal);

        // Assert
        assertEquals("newbie@example.com", capturedSavedUser().getEmail());
    }

    @Test
    @DisplayName("Створення (OIDC): перший вхід адміна — створення запису User з роллю ADMIN")
    void create_oidcFirstLoginAdmin_createsUserWithAdminRole() {
        // Arrange
        Category streaming = category(10L, "Streaming");
        OidcUser principal = oidcUser("boss", "sub-6", "boss@mail.com", "ROLE_ADMIN");
        SubscriptionRequestDto dto = requestDto(null, 10L);
        when(userRepository.findByUsername("boss")).thenReturn(Optional.empty());
        stubUserSaveAssigningId(6L);
        stubCreatePipeline(dto, streaming, new Subscription(), responseDto(100L));

        // Act
        subscriptionService.create(dto, principal);

        // Assert
        assertEquals("ADMIN", capturedSavedUser().getRole());
    }

    @Test
    @DisplayName("Створення (OIDC): за відсутності preferred_username використовується Name")
    void create_oidcWithoutPreferredUsername_usesPrincipalName() {
        // Arrange
        User currentUser = user(9L, "sub-9", "USER");
        Category streaming = category(10L, "Streaming");
        OidcUser principal = oidcUser(null, "sub-9", "x@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(null, 10L);
        when(userRepository.findByUsername("sub-9")).thenReturn(Optional.of(currentUser));
        stubCreatePipeline(dto, streaming, new Subscription(), responseDto(100L));

        // Act
        subscriptionService.create(dto, principal);

        // Assert
        verify(userRepository).findByUsername("sub-9");
        assertSame(currentUser, capturedSavedSubscription().getUser());
    }

    // =====================================================================
    // update(id, dto, OidcUser)
    // =====================================================================

    @Test
    @DisplayName("Оновлення (OIDC): власник підписки успішно оновлює свої дані")
    void update_oidcOwner_updatesEntityAndSaves() {
        // Arrange
        User owner = user(1L, "evgen", "USER");
        Category oldCategory = category(5L, "Old");
        Category streaming = category(10L, "Streaming");
        Subscription existing = subscription(7L, owner, oldCategory);
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(1L, 10L);
        SubscriptionResponseDto expected = responseDto(7L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(streaming));
        when(subscriptionRepository.save(existing)).thenReturn(existing);
        when(subscriptionMapper.toDto(existing)).thenReturn(expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.update(7L, dto, principal);

        // Assert
        assertSame(expected, result);
        verify(subscriptionMapper).updateEntityFromDto(dto, existing);
        Subscription saved = capturedSavedSubscription();
        assertSame(streaming, saved.getCategory());
        assertSame(owner, saved.getUser());
    }

    @Test
    @DisplayName("Оновлення (OIDC): спроба оновити чужу підписку — AccessDeniedException")
    void update_oidcOtherUsersSubscription_throwsAccessDenied() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        User otherOwner = user(2L, "other", "USER");
        Subscription existing = subscription(7L, otherOwner, category(5L, "Old"));
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(1L, 10L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> subscriptionService.update(7L, dto, principal));
        verify(subscriptionMapper, never()).updateEntityFromDto(any(), any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Оновлення (OIDC): власник намагається змінити власника підписки на іншого — AccessDeniedException")
    void update_oidcOwnerTriesToChangeOwner_throwsAccessDenied() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        Subscription existing = subscription(7L, currentUser, category(5L, "Old"));
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(2L, 10L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> subscriptionService.update(7L, dto, principal));
        verify(subscriptionMapper, never()).updateEntityFromDto(any(), any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Оновлення (OIDC): підписку за вказаним ID не знайдено — EntityNotFoundException")
    void update_oidcUnknownSubscription_throwsEntityNotFound() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(1L, 10L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> subscriptionService.update(99L, dto, principal));
        assertEquals("Subscription with id 99 was not found", ex.getMessage());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Оновлення (OIDC): адмін оновлює підписку та передає її іншому користувачу — успішно")
    void update_oidcAdminReassignsOwner_setsNewOwner() {
        // Arrange
        User admin = user(100L, "admin", "ADMIN");
        User oldOwner = user(1L, "evgen", "USER");
        User newOwner = user(3L, "other", "USER");
        Category streaming = category(10L, "Streaming");
        Subscription existing = subscription(7L, oldOwner, category(5L, "Old"));
        OidcUser principal = oidcUser("admin", "sub-100", "admin@mail.com", "ROLE_ADMIN");
        SubscriptionRequestDto dto = requestDto(3L, 10L);
        SubscriptionResponseDto expected = responseDto(7L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newOwner));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(streaming));
        when(subscriptionRepository.save(existing)).thenReturn(existing);
        when(subscriptionMapper.toDto(existing)).thenReturn(expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.update(7L, dto, principal);

        // Assert
        assertSame(expected, result);
        assertSame(newOwner, capturedSavedSubscription().getUser());
    }

    @Test
    @DisplayName("Оновлення (OIDC): вказано неіснуючу категорію — EntityNotFoundException")
    void update_oidcUnknownCategory_throwsEntityNotFound() {
        // Arrange
        User owner = user(1L, "evgen", "USER");
        Subscription existing = subscription(7L, owner, category(5L, "Old"));
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        SubscriptionRequestDto dto = requestDto(1L, 99L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> subscriptionService.update(7L, dto, principal));
        assertEquals("Category with id 99 was not found", ex.getMessage());
        verify(subscriptionRepository, never()).save(any());
    }

    // =====================================================================
    // delete(id, OidcUser)
    // =====================================================================

    @Test
    @DisplayName("Видалення (OIDC): власник успішно видаляє свою підписку")
    void delete_oidcOwner_deletesSubscription() {
        // Arrange
        User owner = user(1L, "evgen", "USER");
        Subscription existing = subscription(7L, owner, category(10L, "Streaming"));
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        // Act
        subscriptionService.delete(7L, principal);

        // Assert
        verify(subscriptionRepository).delete(existing);
    }

    @Test
    @DisplayName("Видалення (OIDC): спроба видалити чужу підписку звичайним користувачем — AccessDeniedException")
    void delete_oidcOtherUsersSubscription_throwsAccessDenied() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        User otherOwner = user(2L, "other", "USER");
        Subscription existing = subscription(7L, otherOwner, category(10L, "Streaming"));
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> subscriptionService.delete(7L, principal));
        verify(subscriptionRepository, never()).delete(any(Subscription.class));
    }

    @Test
    @DisplayName("Видалення (OIDC): адмін видаляє чужу підписку — успішно")
    void delete_oidcAdminOtherUsersSubscription_deletesSubscription() {
        // Arrange
        User admin = user(100L, "admin", "ADMIN");
        User otherOwner = user(2L, "other", "USER");
        Subscription existing = subscription(7L, otherOwner, category(10L, "Streaming"));
        OidcUser principal = oidcUser("admin", "sub-100", "admin@mail.com", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        // Act
        subscriptionService.delete(7L, principal);

        // Assert
        verify(subscriptionRepository).delete(existing);
    }

    @Test
    @DisplayName("Видалення (OIDC): неіснуючий ID підписки — EntityNotFoundException")
    void delete_oidcUnknownSubscription_throwsEntityNotFound() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        OidcUser principal = oidcUser("evgen", "sub-1", "evgen@mail.com", "ROLE_USER");
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> subscriptionService.delete(99L, principal));
        assertEquals("Subscription with id 99 was not found", ex.getMessage());
        verify(subscriptionRepository, never()).delete(any(Subscription.class));
    }

    // =====================================================================
    // create(dto, Jwt)
    // =====================================================================

    @Test
    @DisplayName("Створення (JWT): звичайний користувач створює підписку для себе — успішно")
    void create_jwtRegularUserForSelf_savesSubscriptionOwnedByCurrentUser() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        Category streaming = category(10L, "Streaming");
        Jwt jwt = jwt(jwtClaims("evgen", "evgen@mail.com"));
        SubscriptionRequestDto dto = requestDto(1L, 10L);
        Subscription mapped = new Subscription();
        SubscriptionResponseDto expected = responseDto(100L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        stubCreatePipeline(dto, streaming, mapped, expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.create(dto, jwt);

        // Assert
        assertSame(expected, result);
        Subscription saved = capturedSavedSubscription();
        assertSame(currentUser, saved.getUser());
        assertSame(streaming, saved.getCategory());
    }

    @Test
    @DisplayName("Створення (JWT): спроба створити підписку для чужого ID — AccessDeniedException")
    void create_jwtRegularUserForAnotherUser_throwsAccessDenied() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        Jwt jwt = jwt(jwtClaims("evgen", "evgen@mail.com"));
        SubscriptionRequestDto dto = requestDto(2L, 10L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> subscriptionService.create(dto, jwt));
        verifyNoInteractions(subscriptionRepository, categoryRepository);
    }

    @Test
    @DisplayName("Створення (JWT): адмін із 'roles' claim створює підписку для іншого користувача")
    void create_jwtAdminViaRolesClaim_canAssignAnotherUser() {
        // Arrange
        User admin = user(100L, "admin", "ADMIN");
        User target = user(2L, "evgen", "USER");
        Category streaming = category(10L, "Streaming");
        Map<String, Object> claims = jwtClaims("admin", "admin@mail.com");
        claims.put("roles", List.of("ADMIN"));
        Jwt jwt = jwt(claims);
        SubscriptionRequestDto dto = requestDto(2L, 10L);
        SubscriptionResponseDto expected = responseDto(100L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        stubCreatePipeline(dto, streaming, new Subscription(), expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.create(dto, jwt);

        // Assert
        assertSame(expected, result);
        assertSame(target, capturedSavedSubscription().getUser());
    }

    @Test
    @DisplayName("Створення (JWT): адмін із Keycloak 'realm_access.roles' створює підписку для іншого")
    void create_jwtAdminViaRealmAccessRoles_canAssignAnotherUser() {
        // Arrange
        User admin = user(100L, "admin", "ADMIN");
        User target = user(2L, "evgen", "USER");
        Category streaming = category(10L, "Streaming");
        Map<String, Object> claims = jwtClaims("admin", "admin@mail.com");
        claims.put("realm_access", Map.of("roles", List.of("ADMIN")));
        Jwt jwt = jwt(claims);
        SubscriptionRequestDto dto = requestDto(2L, 10L);
        SubscriptionResponseDto expected = responseDto(100L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        stubCreatePipeline(dto, streaming, new Subscription(), expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.create(dto, jwt);

        // Assert
        assertSame(expected, result);
        assertSame(target, capturedSavedSubscription().getUser());
    }

    @Test
    @DisplayName("Створення (JWT): роль 'admin' у нижньому регістрі сприймається як ADMIN")
    void create_jwtRealmAccessLowercaseAdmin_treatedAsAdmin() {
        // Arrange
        User admin = user(100L, "admin", "ADMIN");
        User target = user(2L, "evgen", "USER");
        Category streaming = category(10L, "Streaming");
        Map<String, Object> claims = jwtClaims("admin", "admin@mail.com");
        claims.put("realm_access", Map.of("roles", List.of("admin")));
        Jwt jwt = jwt(claims);
        SubscriptionRequestDto dto = requestDto(2L, 10L);
        SubscriptionResponseDto expected = responseDto(100L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        stubCreatePipeline(dto, streaming, new Subscription(), expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.create(dto, jwt);

        // Assert
        assertSame(expected, result);
        assertSame(target, capturedSavedSubscription().getUser());
    }

    @Test
    @DisplayName("Створення (JWT): токен без ролей сприймається як звичайний USER — AccessDeniedException")
    void create_jwtRealmAccessWithoutRoles_treatedAsRegularUser() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        Map<String, Object> claims = jwtClaims("evgen", "evgen@mail.com");
        claims.put("realm_access", Map.of("other", "value"));
        Jwt jwt = jwt(claims);
        SubscriptionRequestDto dto = requestDto(2L, 10L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> subscriptionService.create(dto, jwt));
        verify(subscriptionMapper, never()).toEntity(any());
    }

    @Test
    @DisplayName("Створення (JWT): перший вхід — автоматичне створення користувача за email claim")
    void create_jwtFirstLogin_createsUserFromEmailClaim() {
        // Arrange
        Category streaming = category(10L, "Streaming");
        Jwt jwt = jwt(jwtClaims("newbie", "newbie@mail.com"));
        SubscriptionRequestDto dto = requestDto(null, 10L);
        when(userRepository.findByUsername("newbie")).thenReturn(Optional.empty());
        stubUserSaveAssigningId(5L);
        stubCreatePipeline(dto, streaming, new Subscription(), responseDto(100L));

        // Act
        subscriptionService.create(dto, jwt);

        // Assert
        User createdUser = capturedSavedUser();
        assertEquals("newbie", createdUser.getUsername());
        assertEquals("newbie@mail.com", createdUser.getEmail());
        assertEquals("USER", createdUser.getRole());
    }

    @Test
    @DisplayName("Створення (JWT): перший вхід без email — використання резервної пошти")
    void create_jwtFirstLoginWithoutEmail_usesFallbackEmail() {
        // Arrange
        Category streaming = category(10L, "Streaming");
        Jwt jwt = jwt(jwtClaims("newbie", null));
        SubscriptionRequestDto dto = requestDto(null, 10L);
        when(userRepository.findByUsername("newbie")).thenReturn(Optional.empty());
        stubUserSaveAssigningId(5L);
        stubCreatePipeline(dto, streaming, new Subscription(), responseDto(100L));

        // Act
        subscriptionService.create(dto, jwt);

        // Assert
        assertEquals("newbie@example.com", capturedSavedUser().getEmail());
    }

    @Test
    @DisplayName("Створення (JWT): перший вхід із порожнім email — використання резервної пошти")
    void create_jwtFirstLoginWithBlankEmail_usesFallbackEmail() {
        // Arrange
        Category streaming = category(10L, "Streaming");
        Jwt jwt = jwt(jwtClaims("newbie", "   "));
        SubscriptionRequestDto dto = requestDto(null, 10L);
        when(userRepository.findByUsername("newbie")).thenReturn(Optional.empty());
        stubUserSaveAssigningId(5L);
        stubCreatePipeline(dto, streaming, new Subscription(), responseDto(100L));

        // Act
        subscriptionService.create(dto, jwt);

        // Assert
        assertEquals("newbie@example.com", capturedSavedUser().getEmail());
    }

    @Test
    @DisplayName("Створення (JWT): перший вхід адміна — створення користувача з роллю ADMIN")
    void create_jwtFirstLoginAdminViaRolesClaim_createsUserWithAdminRole() {
        // Arrange
        Category streaming = category(10L, "Streaming");
        Map<String, Object> claims = jwtClaims("boss", "boss@mail.com");
        claims.put("roles", List.of("ADMIN"));
        Jwt jwt = jwt(claims);
        SubscriptionRequestDto dto = requestDto(null, 10L);
        when(userRepository.findByUsername("boss")).thenReturn(Optional.empty());
        stubUserSaveAssigningId(6L);
        stubCreatePipeline(dto, streaming, new Subscription(), responseDto(100L));

        // Act
        subscriptionService.create(dto, jwt);

        // Assert
        assertEquals("ADMIN", capturedSavedUser().getRole());
    }

    @Test
    @DisplayName("Створення (JWT): за відсутності preferred_username використовується claim 'sub'")
    void create_jwtWithoutPreferredUsername_usesSubject() {
        // Arrange
        User currentUser = user(9L, "subject-123", "USER");
        Category streaming = category(10L, "Streaming");
        Jwt jwt = jwt(jwtClaims(null, "x@mail.com"));
        SubscriptionRequestDto dto = requestDto(null, 10L);
        when(userRepository.findByUsername("subject-123")).thenReturn(Optional.of(currentUser));
        stubCreatePipeline(dto, streaming, new Subscription(), responseDto(100L));

        // Act
        subscriptionService.create(dto, jwt);

        // Assert
        verify(userRepository).findByUsername("subject-123");
        assertSame(currentUser, capturedSavedSubscription().getUser());
    }

    // =====================================================================
    // update(id, dto, Jwt)
    // =====================================================================

    @Test
    @DisplayName("Оновлення (JWT): власник успішно оновлює свою підписку")
    void update_jwtOwner_updatesEntityAndSaves() {
        // Arrange
        User owner = user(1L, "evgen", "USER");
        Category streaming = category(10L, "Streaming");
        Subscription existing = subscription(7L, owner, category(5L, "Old"));
        Jwt jwt = jwt(jwtClaims("evgen", "evgen@mail.com"));
        SubscriptionRequestDto dto = requestDto(1L, 10L);
        SubscriptionResponseDto expected = responseDto(7L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(streaming));
        when(subscriptionRepository.save(existing)).thenReturn(existing);
        when(subscriptionMapper.toDto(existing)).thenReturn(expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.update(7L, dto, jwt);

        // Assert
        assertSame(expected, result);
        verify(subscriptionMapper).updateEntityFromDto(dto, existing);
        assertSame(streaming, capturedSavedSubscription().getCategory());
    }

    @Test
    @DisplayName("Оновлення (JWT): спроба оновити чужу підписку звичайним користувачем — AccessDeniedException")
    void update_jwtOtherUsersSubscription_throwsAccessDenied() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        User otherOwner = user(2L, "other", "USER");
        Subscription existing = subscription(7L, otherOwner, category(5L, "Old"));
        Jwt jwt = jwt(jwtClaims("evgen", "evgen@mail.com"));
        SubscriptionRequestDto dto = requestDto(1L, 10L);
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> subscriptionService.update(7L, dto, jwt));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Оновлення (JWT): адмін перепризначає власника підписки на іншого — успішно")
    void update_jwtAdminReassignsOwner_setsNewOwner() {
        // Arrange
        User admin = user(100L, "admin", "ADMIN");
        User oldOwner = user(1L, "evgen", "USER");
        User newOwner = user(3L, "other", "USER");
        Category streaming = category(10L, "Streaming");
        Subscription existing = subscription(7L, oldOwner, category(5L, "Old"));
        Map<String, Object> claims = jwtClaims("admin", "admin@mail.com");
        claims.put("roles", List.of("ADMIN"));
        Jwt jwt = jwt(claims);
        SubscriptionRequestDto dto = requestDto(3L, 10L);
        SubscriptionResponseDto expected = responseDto(7L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newOwner));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(streaming));
        when(subscriptionRepository.save(existing)).thenReturn(existing);
        when(subscriptionMapper.toDto(existing)).thenReturn(expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.update(7L, dto, jwt);

        // Assert
        assertSame(expected, result);
        assertSame(newOwner, capturedSavedSubscription().getUser());
    }

    // =====================================================================
    // delete(id, Jwt)
    // =====================================================================

    @Test
    @DisplayName("Видалення (JWT): власник успішно видаляє свою підписку")
    void delete_jwtOwner_deletesSubscription() {
        // Arrange
        User owner = user(1L, "evgen", "USER");
        Subscription existing = subscription(7L, owner, category(10L, "Streaming"));
        Jwt jwt = jwt(jwtClaims("evgen", "evgen@mail.com"));
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        // Act
        subscriptionService.delete(7L, jwt);

        // Assert
        verify(subscriptionRepository).delete(existing);
    }

    @Test
    @DisplayName("Видалення (JWT): спроба видалити чужу підписку звичайним користувачем — AccessDeniedException")
    void delete_jwtOtherUsersSubscription_throwsAccessDenied() {
        // Arrange
        User currentUser = user(1L, "evgen", "USER");
        User otherOwner = user(2L, "other", "USER");
        Subscription existing = subscription(7L, otherOwner, category(10L, "Streaming"));
        Jwt jwt = jwt(jwtClaims("evgen", "evgen@mail.com"));
        when(userRepository.findByUsername("evgen")).thenReturn(Optional.of(currentUser));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> subscriptionService.delete(7L, jwt));
        verify(subscriptionRepository, never()).delete(any(Subscription.class));
    }

    @Test
    @DisplayName("Видалення (JWT): адмін успішно видаляє чужу підписку")
    void delete_jwtAdminOtherUsersSubscription_deletesSubscription() {
        // Arrange
        User admin = user(100L, "admin", "ADMIN");
        User otherOwner = user(2L, "other", "USER");
        Subscription existing = subscription(7L, otherOwner, category(10L, "Streaming"));
        Map<String, Object> claims = jwtClaims("admin", "admin@mail.com");
        claims.put("roles", List.of("ADMIN"));
        Jwt jwt = jwt(claims);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        // Act
        subscriptionService.delete(7L, jwt);

        // Assert
        verify(subscriptionRepository).delete(existing);
    }

    // =====================================================================
    // findById
    // =====================================================================

    @Test
    @DisplayName("Пошук за ID: повертає DTO існуючої підписки")
    void findById_existingSubscription_returnsMappedDto() {
        // Arrange
        User owner = user(1L, "evgen", "USER");
        Subscription existing = subscription(7L, owner, category(10L, "Streaming"));
        SubscriptionResponseDto expected = responseDto(7L);
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(subscriptionMapper.toDto(existing)).thenReturn(expected);

        // Act
        SubscriptionResponseDto result = subscriptionService.findById(7L);

        // Assert
        assertSame(expected, result);
    }

    @Test
    @DisplayName("Пошук за ID: неіснуючий ID підписки — EntityNotFoundException")
    void findById_unknownSubscription_throwsEntityNotFound() {
        // Arrange
        when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> subscriptionService.findById(99L));
        assertEquals("Subscription with id 99 was not found", ex.getMessage());
        verifyNoInteractions(subscriptionMapper);
    }

    // =====================================================================
    // findAll
    // =====================================================================

    @Test
    @DisplayName("Пошук всіх: повертає список усіх підписок у правильному порядку")
    void findAll_withSubscriptions_returnsMappedListInOrder() {
        // Arrange
        User owner = user(1L, "evgen", "USER");
        Subscription first = subscription(1L, owner, category(10L, "Streaming"));
        Subscription second = subscription(2L, owner, category(10L, "Streaming"));
        SubscriptionResponseDto firstDto = responseDto(1L);
        SubscriptionResponseDto secondDto = responseDto(2L);
        when(subscriptionRepository.findAll()).thenReturn(List.of(first, second));
        when(subscriptionMapper.toDto(first)).thenReturn(firstDto);
        when(subscriptionMapper.toDto(second)).thenReturn(secondDto);

        // Act
        List<SubscriptionResponseDto> result = subscriptionService.findAll();

        // Assert
        assertEquals(2, result.size());
        assertSame(firstDto, result.get(0));
        assertSame(secondDto, result.get(1));
    }

    @Test
    @DisplayName("Пошук всіх: якщо підписок немає, повертає порожній список")
    void findAll_noSubscriptions_returnsEmptyList() {
        // Arrange
        when(subscriptionRepository.findAll()).thenReturn(List.of());

        // Act
        List<SubscriptionResponseDto> result = subscriptionService.findAll();

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(subscriptionMapper);
    }

    // =====================================================================
    // findAllByUser
    // =====================================================================

    @Test
    @DisplayName("Пошук за користувачем: повертає список підписок конкретного користувача")
    void findAllByUser_userHasSubscriptions_returnsMappedList() {
        // Arrange
        User owner = user(1L, "evgen", "USER");
        Subscription first = subscription(1L, owner, category(10L, "Streaming"));
        Subscription second = subscription(2L, owner, category(10L, "Streaming"));
        SubscriptionResponseDto firstDto = responseDto(1L);
        SubscriptionResponseDto secondDto = responseDto(2L);
        when(subscriptionRepository.findAllByUserId(1L)).thenReturn(List.of(first, second));
        when(subscriptionMapper.toDto(first)).thenReturn(firstDto);
        when(subscriptionMapper.toDto(second)).thenReturn(secondDto);

        // Act
        List<SubscriptionResponseDto> result = subscriptionService.findAllByUser(1L);

        // Assert
        assertEquals(2, result.size());
        assertSame(firstDto, result.get(0));
        assertSame(secondDto, result.get(1));
    }

    @Test
    @DisplayName("Пошук за користувачем: якщо у користувача немає підписок, повертає порожній список")
    void findAllByUser_userHasNoSubscriptions_returnsEmptyList() {
        // Arrange
        when(subscriptionRepository.findAllByUserId(5L)).thenReturn(List.of());

        // Act
        List<SubscriptionResponseDto> result = subscriptionService.findAllByUser(5L);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(subscriptionMapper);
    }

    // =====================================================================
    // deleteAnySubscription
    // =====================================================================

    @Test
    @DisplayName("Пряме видалення: успішно видаляє існуючу підписку")
    void deleteAnySubscription_existingSubscription_deletesIt() {
        // Arrange
        Subscription existing = subscription(7L, user(2L, "other", "USER"), category(10L, "Streaming"));
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(existing));

        // Act
        subscriptionService.deleteAnySubscription(7L);

        // Assert
        verify(subscriptionRepository).delete(existing);
    }

    @Test
    @DisplayName("Пряме видалення: неіснуючий ID підписки — EntityNotFoundException")
    void deleteAnySubscription_unknownSubscription_throwsEntityNotFound() {
        // Arrange
        when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> subscriptionService.deleteAnySubscription(99L));
        assertEquals("Subscription with id 99 was not found", ex.getMessage());
        verify(subscriptionRepository, never()).delete(any(Subscription.class));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void stubCreatePipeline(SubscriptionRequestDto dto, Category category,
                                    Subscription mapped, SubscriptionResponseDto response) {
        when(subscriptionMapper.toEntity(dto)).thenReturn(mapped);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(subscriptionRepository.save(mapped)).thenReturn(mapped);
        when(subscriptionMapper.toDto(mapped)).thenReturn(response);
    }

    private void stubUserSaveAssigningId(Long id) {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(id);
            return saved;
        });
    }

    private Subscription capturedSavedSubscription() {
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        return captor.getValue();
    }

    private User capturedSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private static User user(Long id, String username, String role) {
        return new User(id, username, username + "@mail.com", role);
    }

    private static Category category(Long id, String name) {
        return new Category(id, name);
    }

    private static Subscription subscription(Long id, User owner, Category category) {
        return new Subscription(id, "Netflix", new BigDecimal("9.99"), BILLING_DATE, owner, category);
    }

    private static SubscriptionRequestDto requestDto(Long userId, Long categoryId) {
        SubscriptionRequestDto dto = new SubscriptionRequestDto();
        dto.setName("Netflix");
        dto.setPrice(new BigDecimal("9.99"));
        dto.setBillingDate(BILLING_DATE);
        dto.setUserId(userId);
        dto.setCategoryId(categoryId);
        return dto;
    }

    private static SubscriptionResponseDto responseDto(Long id) {
        SubscriptionResponseDto dto = new SubscriptionResponseDto();
        dto.setId(id);
        return dto;
    }

    private OidcUser oidcUser(String preferredUsername, String name, String email, String... authorities) {
        List<GrantedAuthority> granted = Arrays.stream(authorities)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
        OidcUser principal = mock(OidcUser.class);
        lenient().when(principal.getPreferredUsername()).thenReturn(preferredUsername);
        lenient().when(principal.getName()).thenReturn(name);
        lenient().when(principal.getEmail()).thenReturn(email);
        lenient().doReturn(granted).when(principal).getAuthorities();
        return principal;
    }

    private static Map<String, Object> jwtClaims(String preferredUsername, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "subject-123");
        if (preferredUsername != null) {
            claims.put("preferred_username", preferredUsername);
        }
        if (email != null) {
            claims.put("email", email);
        }
        return claims;
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt("token-value", ISSUED_AT, EXPIRES_AT, Map.of("alg", "none"), claims);
    }
}