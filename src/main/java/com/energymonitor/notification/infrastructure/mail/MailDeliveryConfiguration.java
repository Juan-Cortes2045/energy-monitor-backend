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
 * <p>No queue of its own is declared here: the verification and recovery codes arrive from the
 * security service over the message broker, and {@code MailRequestsConsumer} mails them on the
 * listener thread, so a request of that service never waits for SMTP.
 *
 * <p>This class keeps only what is about the delivery record: the sweep that closes
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