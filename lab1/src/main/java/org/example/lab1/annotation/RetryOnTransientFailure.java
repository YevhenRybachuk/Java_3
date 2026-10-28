package org.example.lab1.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Дозволяє повторити метод, якщо сталася тимчасова помилка доступу до даних. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RetryOnTransientFailure {
    /** Максимальна кількість спроб виконати метод, включно з першою. */
    int maxAttempts() default 3;
}
