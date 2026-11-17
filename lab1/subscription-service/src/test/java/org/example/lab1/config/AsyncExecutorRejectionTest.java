package org.example.lab1.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.example.lab1.service.SubscriptionAsyncTasks;
import org.example.lab1.exception.GlobalApiExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncExecutorRejectionTest {

    @Test
    @DisplayName("Обробник фіксує виняток асинхронного void-методу")
    void uncaughtVoidFailureIsLoggedByConfiguredHandler() throws NoSuchMethodException {
        var handler = new AsyncConfiguration().getAsyncUncaughtExceptionHandler();
        var method = SubscriptionAsyncTasks.class.getMethod("sendSubscriptionCreatedEmail", String.class, String.class);

        assertDoesNotThrow(() -> handler.handleUncaughtException(
                new IllegalStateException("помилка доставки"), method,
                new Object[]{"user@example.com", "Music"}));
    }

    @Test
    @DisplayName("Переповнення обмеженого пулу дає 503 із повідомленням спробувати пізніше")
    void rejectedTaskIsHandledAsServiceUnavailable() throws InterruptedException {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("tiny-async-");
        executor.initialize();

        CountDownLatch runningTaskStarted = new CountDownLatch(1);
        CountDownLatch releaseRunningTask = new CountDownLatch(1);
        try {
            executor.execute(() -> {
                runningTaskStarted.countDown();
                try {
                    releaseRunningTask.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(runningTaskStarted.await(1, TimeUnit.SECONDS));
            executor.execute(() -> { }); // Єдине місце черги тепер зайняте.

            TaskRejectedException rejection = assertThrows(TaskRejectedException.class,
                    () -> executor.execute(() -> { }));
            var response = new GlobalApiExceptionHandler().handleTaskRejected(rejection);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertEquals("Service Unavailable", response.getBody().get("error"));
            assertTrue(response.getBody().get("message").contains("Спробуйте виконати запит пізніше"));
        } finally {
            releaseRunningTask.countDown();
            executor.shutdown();
        }
    }
}
