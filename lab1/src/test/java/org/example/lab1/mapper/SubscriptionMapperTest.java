package org.example.lab1.mapper;

import org.example.lab1.dto.SubscriptionRequestDto;
import org.example.lab1.dto.SubscriptionResponseDto;
import org.example.lab1.entity.Category;
import org.example.lab1.entity.Subscription;
import org.example.lab1.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionMapperTest {

    private final SubscriptionMapper mapper = Mappers.getMapper(SubscriptionMapper.class);

    @Test
    @DisplayName("Мапер: конвертація DTO у Entity — коректне мапінг усіх полів")
    void toEntity_validDto_mapsAllFields() {
        // Arrange
        SubscriptionRequestDto dto = new SubscriptionRequestDto();
        dto.setName("Netflix");
        dto.setPrice(new BigDecimal("9.99"));
        dto.setBillingDate(LocalDate.of(2030, 1, 1));

        // Act
        Subscription entity = mapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals("Netflix", entity.getName());
        assertEquals(new BigDecimal("9.99"), entity.getPrice());
        assertEquals(LocalDate.of(2030, 1, 1), entity.getBillingDate());
    }

    @Test
    @DisplayName("Мапер: конвертація Entity у DTO — коректне мапінг усіх полів")
    void toDto_validEntity_mapsAllFields() {
        // Arrange
        User user = new User(1L, "evgen", "evgen@mail.com", "USER");
        Category category = new Category(10L, "Streaming");
        Subscription entity = new Subscription(100L, "Netflix", new BigDecimal("9.99"), LocalDate.of(2030, 1, 1), user, category);

        // Act
        SubscriptionResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals("Netflix", dto.getName());
        assertEquals(1L, dto.getUserId());
        assertEquals("evgen", dto.getUsername());
        assertEquals(10L, dto.getCategoryId());
        assertEquals("Streaming", dto.getCategoryName());
    }

    @Test
    @DisplayName("Мапер: оновлення Entity з DTO — поля успішно перезаписуються")
    void updateEntityFromDto_validDto_updatesFields() {
        // Arrange
        Subscription entity = new Subscription();
        entity.setName("Old Name");

        SubscriptionRequestDto dto = new SubscriptionRequestDto();
        dto.setName("New Name");
        dto.setPrice(new BigDecimal("15.00"));

        // Act
        mapper.updateEntityFromDto(dto, entity);

        // Assert
        assertEquals("New Name", entity.getName());
        assertEquals(new BigDecimal("15.00"), entity.getPrice());
    }
}