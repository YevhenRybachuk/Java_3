package org.example.lab1.service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Імітує затримку зовнішнього сервісу; тривалість можна змінити для локальних запусків і тестів. */
@Component
public class SlowDomainOperation {
    private final long delayMillis;
    private final AtomicInteger activeCalls = new AtomicInteger();
    private final AtomicInteger maximumConcurrentCalls = new AtomicInteger();
    private final AtomicReference<String> lastThreadName = new AtomicReference<>();

    public SlowDomainOperation(@Value("${app.async.simulated-delay-ms:1000}") long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public void waitForExternalService() {
        int active = activeCalls.incrementAndGet();
        maximumConcurrentCalls.accumulateAndGet(active, Math::max);
        lastThreadName.set(Thread.currentThread().getName());
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Асинхронну операцію перервано", ex);
        } finally {
            activeCalls.decrementAndGet();
        }
    }

    int getMaximumConcurrentCalls() {
        return maximumConcurrentCalls.get();
    }

    String getLastThreadName() {
        return lastThreadName.get();
    }
}
