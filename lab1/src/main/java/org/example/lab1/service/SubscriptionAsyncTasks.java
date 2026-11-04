package org.example.lab1.service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Повільні зовнішні операції винесені в окремий бін, щоб виклики проходили через Spring-проксі. */
@Service
public class SubscriptionAsyncTasks {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionAsyncTasks.class);

    private final SlowDomainOperation slowDomainOperation;

    public SubscriptionAsyncTasks(SlowDomainOperation slowDomainOperation) {
        this.slowDomainOperation = slowDomainOperation;
    }

    /** Надсилає повідомлення про створення підписки у режимі «запустив і забув». */
    @Async("subscriptionTaskExecutor")
    public void sendSubscriptionCreatedEmail(String email, String subscriptionName) {
        slowDomainOperation.waitForExternalService();
        log.info("Надіслано повідомлення {} про створення підписки {}", email, subscriptionName);
    }

    /** Отримує статус платіжного провайдера; це перший незалежний повільний виклик. */
    @Async("subscriptionTaskExecutor")
    public CompletableFuture<String> checkPaymentProvider(Long subscriptionId) {
        slowDomainOperation.waitForExternalService();
        return CompletableFuture.completedFuture("READY-" + subscriptionId);
    }

    /** Розраховує прогноз місячних витрат; це другий незалежний повільний виклик. */
    @Async("subscriptionTaskExecutor")
    public CompletableFuture<BigDecimal> calculateMonthlyForecast(Long userId) {
        slowDomainOperation.waitForExternalService();
        return CompletableFuture.completedFuture(BigDecimal.valueOf(userId).multiply(BigDecimal.TEN));
    }
}
