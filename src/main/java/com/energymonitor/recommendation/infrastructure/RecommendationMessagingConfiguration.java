package com.energymonitor.recommendation.infrastructure;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The topic exchange the recommendations are announced to ({@code recommendation.created}). Only
 * the exchange: the notification service declares and binds its own queue.
 */
@Configuration(proxyBeanMethods = false)
public class RecommendationMessagingConfiguration {

    @Bean
    public TopicExchange recommendationExchange(
            @Value("${messaging.recommendation-exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }
}
