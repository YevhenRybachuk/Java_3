package org.example.lab1.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.example.lab1.validation.ValidSubscriptionRequest; // <-- Імпорт вашої анотації

@Getter
@Setter
@ValidSubscriptionRequest // <-- AC1: Кастомна анотація на рівні класу
public class SubscriptionRequestDto {

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private BigDecimal price;

    @NotNull
    @FutureOrPresent
    private LocalDate billingDate;

    private Long userId;

    @NotNull
    private Long categoryId;
}