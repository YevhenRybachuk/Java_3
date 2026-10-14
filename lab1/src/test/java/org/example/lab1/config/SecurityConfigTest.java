package org.example.lab1.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("jwtAuthenticationConverter: витягує ролі з realm_access та додає префікс ROLE_")
    void jwtAuthenticationConverter_validRealmAccess_convertsToRoleAuthorities() {
        // Arrange
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("realm_access", Map.of("roles", List.of("ADMIN", "ROLE_USER")))
        );

        // Act
        AbstractAuthenticationToken authentication = converter.convert(jwt);

        // Assert
        assertNotNull(authentication);
        Collection<GrantedAuthority> authorities = authentication.getAuthorities();

        List<String> roleNames = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        assertEquals(2, roleNames.size());
        assertTrue(roleNames.contains("ROLE_ADMIN"));
        assertTrue(roleNames.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("jwtAuthenticationConverter: не додає ролі, якщо claim realm_access відсутній")
    void jwtAuthenticationConverter_missingRealmAccess_returnsNoRoleAuthorities() {
        // Arrange
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", "user123")
        );

        // Act
        AbstractAuthenticationToken authentication = converter.convert(jwt);

        // Assert
        assertNotNull(authentication);
        List<String> roleNames = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        assertTrue(roleNames.isEmpty());
    }

    @Test
    @DisplayName("jwtAuthenticationConverter: не додає ролі, якщо roles не є колекцією")
    void jwtAuthenticationConverter_invalidRolesField_returnsNoRoleAuthorities() {
        // Arrange
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("realm_access", Map.of("roles", "invalid_type"))
        );

        // Act
        AbstractAuthenticationToken authentication = converter.convert(jwt);

        // Assert
        assertNotNull(authentication);
        List<String> roleNames = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        assertTrue(roleNames.isEmpty());
    }
}