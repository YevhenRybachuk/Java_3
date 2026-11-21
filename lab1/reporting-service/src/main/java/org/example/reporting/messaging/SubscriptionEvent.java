package org.example.reporting.messaging;

import java.math.BigDecimal;

/** Local copy of the broker contract; this service does not depend on the producer module. */
public record SubscriptionEvent(Long subscriptionId, Long userId, String name,
                                BigDecimal price, String action) { }
