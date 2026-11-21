package org.example.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import org.example.reporting.messaging.SubscriptionEvent;
import org.junit.jupiter.api.Test;

class ReportingServiceTest {
    @Test
    void aggregatesSubscriptionsReceivedAsBrokerEvents() {
        ReportingService service = new ReportingService();
        service.apply(new SubscriptionEvent(1L, 42L, "Video", new BigDecimal("12.50"), "UPSERT"));
        service.apply(new SubscriptionEvent(2L, 42L, "Music", new BigDecimal("7.50"), "UPSERT"));

        SubscriptionReport report = service.summary(42L);

        assertThat(report.subscriptions()).isEqualTo(2);
        assertThat(report.totalPrice()).isEqualByComparingTo("20.00");
    }
}
