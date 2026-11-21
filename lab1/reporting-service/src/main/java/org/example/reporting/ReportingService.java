package org.example.reporting;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.example.reporting.messaging.SubscriptionEvent;
import org.springframework.stereotype.Service;

@Service
public class ReportingService {
    private final ConcurrentMap<Long, SubscriptionEvent> activeSubscriptions = new ConcurrentHashMap<>();

    public void apply(SubscriptionEvent event) {
        if ("DELETE".equals(event.action())) {
            activeSubscriptions.remove(event.subscriptionId());
        } else {
            activeSubscriptions.put(event.subscriptionId(), event);
        }
    }

    SubscriptionReport summary(Long userId) {
        var subscriptions = activeSubscriptions.values().stream()
                .filter(subscription -> subscription.userId().equals(userId))
                .toList();
        BigDecimal total = subscriptions.stream().map(SubscriptionEvent::price)
                .filter(price -> price != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SubscriptionReport(subscriptions.size(), total);
    }
}
