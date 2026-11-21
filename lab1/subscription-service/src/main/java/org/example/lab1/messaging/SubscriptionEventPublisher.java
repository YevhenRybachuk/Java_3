package org.example.lab1.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SubscriptionEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publish(SubscriptionEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMessagingConfiguration.EXCHANGE,
                    RabbitMessagingConfiguration.ROUTING_KEY, objectMapper.writeValueAsString(event), message -> {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    });
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize subscription event", exception);
        }
    }
}
