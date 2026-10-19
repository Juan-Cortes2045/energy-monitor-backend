package com.energymonitor.notification.infrastructure.mail;

import com.energymonitor.notification.infrastructure.MailDeliveryProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wiring for the delivery record's own clock and sweep.
 *
 * <p>The queue that delivery runs on is not declared here. It belongs to
 * {@code com.energymonitor.security.infrastructure.SecurityResetExecutorConfiguration}, because
 * what it protects is a property of the endpoint rather than of mail: the recovery endpoint answers
 * every caller identically, and the queue is what keeps the work that could reveal a difference off
 * the request thread. Mail delivery is one consumer of that queue, not its owner, and giving this
 * module a second, private pool would mean two workers competing over the same backpressure and
 * neither of them able to explain a refusal.
 *
 * <p>So this class keeps only what is genuinely about the delivery record: the sweep that closes
 * deliveries abandoned by an earlier run, and the clock that sweep measures age against.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MailDeliveryProperties.class)
@EnableScheduling
public class MailDeliveryConfiguration {

    /**
     * Time source for delivery records, so the sweep's threshold can be tested by moving the clock
     * rather than by waiting.
     *
     * @return a UTC clock
     */
    @Bean
    public Clock mailDeliveryClock() {
        return Clock.systemUTC();
    }
}