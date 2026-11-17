package org.example.reporting;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
class ReportingService {
    private final RestClient subscriptionClient;

    ReportingService(RestClient.Builder builder,
                     @Value("${services.subscription.base-url}") String subscriptionBaseUrl) {
        this.subscriptionClient = builder.baseUrl(subscriptionBaseUrl).build();
    }

    SubscriptionReport summary(Long userId, String accessToken) {
        List<SubscriptionItem> subscriptions = subscriptionClient.get()
                .uri(uri -> uri.path("/api/subscriptions").queryParam("userId", userId).build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() { });
        if (subscriptions == null) return new SubscriptionReport(0, BigDecimal.ZERO);
        BigDecimal total = subscriptions.stream().map(SubscriptionItem::price)
                .filter(price -> price != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SubscriptionReport(subscriptions.size(), total);
    }

    private record SubscriptionItem(BigDecimal price) { }
}
