package org.example.lab1.messaging;

import java.math.BigDecimal;

/** Event contract published when a subscription changes. Kept independent from the reporting service. */
public record SubscriptionEvent(Long subscriptionId, Long userId, String name,
                                BigDecimal price, String action) { }
