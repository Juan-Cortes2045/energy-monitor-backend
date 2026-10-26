package com.energymonitor.alert.infrastructure;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The topic exchange the alerts are announced to ({@code alert.raised}). Only the exchange: the
 * notification service declares and binds its own queue.
 */
@Configuration(proxyBeanMethods = false)
public class AlertMessagingConfiguration {

    @Bean
    public TopicExchange alertExchange(@Value("${messaging.alert-exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }
}
