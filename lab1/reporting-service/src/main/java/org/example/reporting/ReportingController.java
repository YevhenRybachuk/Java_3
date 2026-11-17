package org.example.reporting;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReportingController {
    private final ReportingService reportingService;
    ReportingController(ReportingService reportingService) { this.reportingService = reportingService; }

    @GetMapping("/reports/subscriptions/summary")
    SubscriptionReport summary(@RequestParam Long userId, @AuthenticationPrincipal Jwt jwt) {
        return reportingService.summary(userId, jwt.getTokenValue());
    }
}
