package org.example.lab1.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.lab1.dto.SubscriptionRequestDto;

public class SubscriptionRequestValidator implements ConstraintValidator<ValidSubscriptionRequest, SubscriptionRequestDto> {

    @Override
    public boolean isValid(SubscriptionRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true; // Null DTOs обробляються через @NotNull якщо потрібно
        }

        BigDecimal price = dto.getPrice();
        LocalDate billingDate = dto.getBillingDate();

        // Якщо ціна вказана і більше 0, то дата списання є обов'язковою і не може бути в минулому
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            if (billingDate == null) {
                buildCustomViolation(context, "billingDate", "Дата списання обов'язкова для платної підписки");
                return false;
            }
            if (billingDate.isBefore(LocalDate.now())) {
                buildCustomViolation(context, "billingDate", "Дата списання не може бути раніше ніж сьогоднішня дата");
                return false;
            }
        }

        return true;
    }

    private void buildCustomViolation(ConstraintValidatorContext context, String propertyName, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyName)
                .addConstraintViolation();
    }
}