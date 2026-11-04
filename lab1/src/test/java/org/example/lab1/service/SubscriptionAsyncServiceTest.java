package org.example.lab1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.example.lab1.dto.SubscriptionInsightsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubscriptionAsyncServiceTest {

    @Test
    @DisplayName("Асинхронний збій перетворюється на контрольований результат")
    void failedTaskReturnsFallbackInsteadOfFailingRequest() {
        SubscriptionAsyncTasks tasks = mock(SubscriptionAsyncTasks.class);
        when(tasks.checkPaymentProvider(5L))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Провайдер недоступний")));
        when(tasks.calculateMonthlyForecast(8L)).thenReturn(CompletableFuture.completedFuture(BigDecimal.TEN));

        SubscriptionInsightsDto result = new SubscriptionAsyncService(tasks).getInsights(5L, 8L).join();

        assertEquals("UNKNOWN", result.paymentProviderStatus());
        assertEquals("Не вдалося отримати повний звіт; спробуйте пізніше", result.message());
    }

    @Test
    @DisplayName("Очікування асинхронного результату обмежене тайм-аутом")
    void timeoutReturnsFallbackResult() {
        SubscriptionAsyncTasks tasks = mock(SubscriptionAsyncTasks.class);
        when(tasks.checkPaymentProvider(5L)).thenReturn(new CompletableFuture<>());
        when(tasks.calculateMonthlyForecast(8L)).thenReturn(CompletableFuture.completedFuture(BigDecimal.TEN));

        SubscriptionInsightsDto result = new SubscriptionAsyncService(tasks).getInsights(5L, 8L).join();

        assertEquals("UNKNOWN", result.paymentProviderStatus());
        assertEquals("Не вдалося отримати повний звіт; спробуйте пізніше", result.message());
    }
}
