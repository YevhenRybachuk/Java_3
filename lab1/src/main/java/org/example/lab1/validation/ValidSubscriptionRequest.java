package org.example.lab1.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Валідаційна анотація на рівні класу для перевірки коректності даних підписки.
 */
@Documented
@Constraint(validatedBy = SubscriptionRequestValidator.class) // AC1: зв'язуємо з валідатором
@Target({ ElementType.TYPE })                                   // Анотація застосовується до класу (DTO)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSubscriptionRequest {

    String message() default "Дата списання повинна бути вказна та не може бути в минулому для платної підписки";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}