package org.example.reporting;

import java.math.BigDecimal;

public record SubscriptionReport(long subscriptions, BigDecimal totalPrice) { }
