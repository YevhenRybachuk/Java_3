package org.example.lab1.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebConfigTest {

    @Test
    @DisplayName("addArgumentResolvers: успішно реєструє JwtArgumentResolver у списку резолверів WebMVC")
    void addArgumentResolvers_registersJwtArgumentResolver() {
        // Arrange
        WebConfig webConfig = new WebConfig();
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        // Act
        webConfig.addArgumentResolvers(resolvers);

        // Assert
        assertEquals(1, resolvers.size());
        assertTrue(resolvers.get(0) instanceof JwtArgumentResolver);
    }
}