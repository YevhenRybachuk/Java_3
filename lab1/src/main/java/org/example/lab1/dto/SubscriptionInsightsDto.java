package org.example.lab1.dto;

import java.math.BigDecimal;

/** Об'єднані результати перевірки платежів і прогнозу витрат за підпискою. */
public record SubscriptionInsightsDto(
        Long subscriptionId,
        Long userId,
        String paymentProviderStatus,
        BigDecimal monthlyForecast,
        String message) {

    public static SubscriptionInsightsDto unavailable(Long subscriptionId, Long userId) {
        return new SubscriptionInsightsDto(subscriptionId, userId, "UNKNOWN", null,
                "Не вдалося отримати повний звіт; спробуйте пізніше");
    }
}
