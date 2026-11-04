package org.example.lab1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.example.lab1.config.AsyncConfiguration;
import org.example.lab1.dto.SubscriptionInsightsDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncProcessingTest {
    private AnnotationConfigApplicationContext context;

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("Повільна задача працює у власному пулі та не блокує виклик")
    void fireAndForgetRunsOnConfiguredExecutor() throws Exception {
        openContext(500);
        SubscriptionAsyncTasks tasks = context.getBean(SubscriptionAsyncTasks.class);
        SlowDomainOperation operation = context.getBean(SlowDomainOperation.class);

        long startedAt = System.nanoTime();
        tasks.sendSubscriptionCreatedEmail("user@example.com", "Music");
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertTrue(elapsedMillis < 300, "Виклик не повинен чекати на завершення повільної операції");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (operation.getLastThreadName() == null && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertNotNull(operation.getLastThreadName());
        assertTrue(operation.getLastThreadName().startsWith("subscription-async-"));
    }

    @Test
    @DisplayName("Два незалежні виклики виконуються одночасно та об'єднуються в один результат")
    void independentCallsRunConcurrentlyAndAreCombined() {
        openContext(300);
        SubscriptionAsyncTasks tasks = context.getBean(SubscriptionAsyncTasks.class);
        SlowDomainOperation operation = context.getBean(SlowDomainOperation.class);
        ThreadPoolTaskExecutor executor = context.getBean("subscriptionTaskExecutor", ThreadPoolTaskExecutor.class);

        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaxPoolSize());
        assertEquals(8, executor.getThreadPoolExecutor().getQueue().remainingCapacity());

        CompletableFuture<String> provider = tasks.checkPaymentProvider(12L);
        CompletableFuture<java.math.BigDecimal> forecast = tasks.calculateMonthlyForecast(7L);
        SubscriptionInsightsDto insights = provider.thenCombine(forecast,
                (status, amount) -> new SubscriptionInsightsDto(12L, 7L, status, amount, "Звіт сформовано"))
                .join();

        assertEquals("READY-12", insights.paymentProviderStatus());
        assertEquals(0, java.math.BigDecimal.valueOf(70).compareTo(insights.monthlyForecast()));
        assertEquals(2, operation.getMaximumConcurrentCalls());
        assertTrue(operation.getLastThreadName().startsWith("subscription-async-"));
    }

    private void openContext(long simulatedDelayMillis) {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("async-test-properties",
                Map.of("app.async.simulated-delay-ms", simulatedDelayMillis)));
        context.register(AsyncConfiguration.class, SlowDomainOperation.class, SubscriptionAsyncTasks.class);
        context.refresh();
    }
}
