package org.example.reporting;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReportingController {
    private final ReportingService reportingService;
    ReportingController(ReportingService reportingService) { this.reportingService = reportingService; }

    @GetMapping("/reports/subscriptions/summary")
    SubscriptionReport summary(@RequestParam Long userId) {
        return reportingService.summary(userId);
    }
}
