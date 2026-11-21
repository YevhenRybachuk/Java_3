package org.example.reporting.messaging;

import org.example.reporting.ReportingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SubscriptionEventListener {
    private final ObjectMapper objectMapper;
    private final ReportingService reportingService;

    public SubscriptionEventListener(ObjectMapper objectMapper, ReportingService reportingService) {
        this.objectMapper = objectMapper;
        this.reportingService = reportingService;
    }

    @RabbitListener(queues = RabbitMessagingConfiguration.QUEUE)
    public void onSubscriptionChanged(String message) {
        reportingService.apply(objectMapper.readValue(message, SubscriptionEvent.class));
    }
}
