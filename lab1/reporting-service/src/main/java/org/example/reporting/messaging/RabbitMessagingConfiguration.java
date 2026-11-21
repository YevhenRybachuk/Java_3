package org.example.reporting.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfiguration {
    public static final String EXCHANGE = "subscriptions.events";
    public static final String QUEUE = "reporting.subscriptions";
    public static final String ROUTING_KEY = "subscription.changed";

    @Bean
    DirectExchange subscriptionEventsExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue reportingSubscriptionsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding reportingSubscriptionsBinding(Queue reportingSubscriptionsQueue,
                                          DirectExchange subscriptionEventsExchange) {
        return BindingBuilder.bind(reportingSubscriptionsQueue).to(subscriptionEventsExchange).with(ROUTING_KEY);
    }
}
