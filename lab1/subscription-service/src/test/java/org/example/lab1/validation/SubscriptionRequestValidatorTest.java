package org.example.lab1.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.example.lab1.dto.SubscriptionRequestDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionRequestValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // =====================================================================
    // AC2: Валідний DTO не викликає помилок валідації
    // =====================================================================

    @Test
    @DisplayName("AC2: Валідний DTO з додатною ціною та майбутньою датою не викликає помилок constraint")
    void validate_validPaidSubscription_noViolations() {
        // Arrange
        SubscriptionRequestDto dto = new SubscriptionRequestDto();
        dto.setName("Netflix");
        dto.setPrice(new BigDecimal("12.99"));
        dto.setBillingDate(LocalDate.now().plusDays(10));
        dto.setCategoryId(1L);

        // Act
        Set<ConstraintViolation<SubscriptionRequestDto>> violations = validator.validate(dto);

        // Assert
        assertTrue(violations.isEmpty(), "Валідний DTO не повинен мати зауважень валідації");
    }

    // =====================================================================
    // AC3: Невалідний DTO генерує чітке повідомлення про помилку
    // =====================================================================

    @Test
    @DisplayName("AC3: Платна підписка з датою в минулому генерує виразне повідомлення про помилку")
    void validate_pastBillingDateForPaidSubscription_returnsClearViolationMessage() {
        // Arrange
        SubscriptionRequestDto dto = new SubscriptionRequestDto();
        dto.setName("Spotify");
        dto.setPrice(new BigDecimal("9.99"));
        dto.setBillingDate(LocalDate.now().minusDays(5)); // Дата в минулому
        dto.setCategoryId(1L);

        // Act
        Set<ConstraintViolation<SubscriptionRequestDto>> violations = validator.validate(dto);

        // Assert
        assertFalse(violations.isEmpty(), "Має бути виявлено порушення");

        boolean hasExpectedMessage = violations.stream()
                .anyMatch(v -> v.getMessage().contains("Дата списання не може бути раніше ніж сьогоднішня дата")
                        || v.getMessage().contains("має бути в майбутньому"));

        assertTrue(hasExpectedMessage, "Очікувалось повідомлення про помилку для дати в минулому");
    }

    @Test
    @DisplayName("AC3: Платна підписка без дати списання генерує виразне повідомлення про обов'язковість дати")
    void validate_nullBillingDateForPaidSubscription_returnsClearViolationMessage() {
        // Arrange
        SubscriptionRequestDto dto = new SubscriptionRequestDto();
        dto.setName("YouTube Premium");
        dto.setPrice(new BigDecimal("11.99"));
        dto.setBillingDate(null); // Відсутня дата
        dto.setCategoryId(1L);

        // Act
        Set<ConstraintViolation<SubscriptionRequestDto>> violations = validator.validate(dto);

        // Assert
        assertFalse(violations.isEmpty());

        boolean hasExpectedMessage = violations.stream()
                .anyMatch(v -> v.getMessage().contains("Дата списання обов'язкова для платної підписки"));

        assertTrue(hasExpectedMessage, "Повідомлення має чітко пояснювати відсутність дати для платної підписки");
    }
}