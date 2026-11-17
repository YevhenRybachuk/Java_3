package org.example.lab1.config;

import org.example.lab1.annotation.CurrentJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class JwtArgumentResolverTest {

    private JwtArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new JwtArgumentResolver();
        SecurityContextHolder.clearContext();
    }

    void sampleMethod(@CurrentJwt Jwt annotatedJwt, Jwt unannotatedJwt, @CurrentJwt String wrongType) {}

    @Test
    @DisplayName("supportsParameter: повертає true для параметра з @CurrentJwt та типу Jwt")
    void supportsParameter_annotatedJwt_returnsTrue() throws NoSuchMethodException {
        MethodParameter param = new MethodParameter(
                getClass().getDeclaredMethod("sampleMethod", Jwt.class, Jwt.class, String.class), 0);

        assertTrue(resolver.supportsParameter(param));
    }

    @Test
    @DisplayName("supportsParameter: повертає false для параметра без анотації @CurrentJwt")
    void supportsParameter_unannotated_returnsFalse() throws NoSuchMethodException {
        MethodParameter param = new MethodParameter(
                getClass().getDeclaredMethod("sampleMethod", Jwt.class, Jwt.class, String.class), 1);

        assertFalse(resolver.supportsParameter(param));
    }

    @Test
    @DisplayName("supportsParameter: повертає false для параметра з @CurrentJwt невідповідного типу")
    void supportsParameter_wrongType_returnsFalse() throws NoSuchMethodException {
        MethodParameter param = new MethodParameter(
                getClass().getDeclaredMethod("sampleMethod", Jwt.class, Jwt.class, String.class), 2);

        assertFalse(resolver.supportsParameter(param));
    }

    @Test
    @DisplayName("resolveArgument: витягує актуальний Jwt об'єкт із SecurityContext")
    void resolveArgument_jwtInSecurityContext_returnsJwt() throws Exception {
        Jwt jwt = mock(Jwt.class);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        MethodParameter param = new MethodParameter(
                getClass().getDeclaredMethod("sampleMethod", Jwt.class, Jwt.class, String.class), 0);

        Object result = resolver.resolveArgument(param, null, null, null);

        assertSame(jwt, result);
    }

    @Test
    @DisplayName("resolveArgument: повертає null, якщо SecurityContext порожній")
    void resolveArgument_emptySecurityContext_returnsNull() throws Exception {
        MethodParameter param = new MethodParameter(
                getClass().getDeclaredMethod("sampleMethod", Jwt.class, Jwt.class, String.class), 0);

        Object result = resolver.resolveArgument(param, null, null, null);

        assertNull(result);
    }

    @Test
    @DisplayName("resolveArgument: повертає null, якщо principal в SecurityContext не є Jwt")
    void resolveArgument_nonJwtAuthentication_returnsNull() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pass"));

        MethodParameter param = new MethodParameter(
                getClass().getDeclaredMethod("sampleMethod", Jwt.class, Jwt.class, String.class), 0);

        Object result = resolver.resolveArgument(param, null, null, null);

        assertNull(result);
    }
}