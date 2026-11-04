package org.example.lab1.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.example.lab1.dto.SubscriptionInsightsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Запускає незалежні асинхронні операції та об'єднує їхні результати. */
@Service
@RequiredArgsConstructor
public class SubscriptionAsyncService {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionAsyncService.class);
    private static final long RESULT_TIMEOUT_SECONDS = 3;

    private final SubscriptionAsyncTasks asyncTasks;

    public CompletableFuture<SubscriptionInsightsDto> getInsights(Long subscriptionId, Long userId) {
        // Обидві задачі ставляться у власний executor до очікування будь-якого результату.
        CompletableFuture<String> providerStatus = asyncTasks.checkPaymentProvider(subscriptionId);
        CompletableFuture<BigDecimal> monthlyForecast = asyncTasks.calculateMonthlyForecast(userId);

        return providerStatus.thenCombine(monthlyForecast,
                        (status, forecast) -> new SubscriptionInsightsDto(
                                subscriptionId, userId, status, forecast, "Звіт сформовано"))
                .orTimeout(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(exception -> {
                    log.error("Не вдалося сформувати асинхронний звіт для підписки {}", subscriptionId,
                            exception);
                    return SubscriptionInsightsDto.unavailable(subscriptionId, userId);
                });
    }
}
