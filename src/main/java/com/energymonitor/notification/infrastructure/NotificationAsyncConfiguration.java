package com.energymonitor.notification.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Turns on {@code @Async}: alert mail and push go out on Spring Boot's task executor, so the
 * thread that raised the alert does not wait for SMTP or a push service.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class NotificationAsyncConfiguration {
}
