package com.energymonitor.device.infrastructure;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Security of the endpoints the MQTT broker calls.
 *
 * <p>The broker cannot present a JWT, so these paths get their own filter chain, evaluated
 * before the API chain. Instead of a token, the caller's address must belong to
 * {@code mqtt.broker-auth.allowed-networks}. The default covers loopback and the private ranges
 * Docker uses, which is where the broker lives both locally and in a compose deployment.
 *
 * <p>Behind a reverse proxy every request seems to come from the proxy, so the proxy must not
 * forward {@code /internal/**} at all.
 */
@Configuration(proxyBeanMethods = false)
public class BrokerAuthSecurityConfiguration {

    @Bean
    @Order(1)
    public SecurityFilterChain brokerAuthFilterChain(
            HttpSecurity http,
            @Value("${mqtt.broker-auth.allowed-networks:127.0.0.1/32,::1/128,172.16.0.0/12}") String allowedNetworks)
            throws Exception {
        List<IpAddressMatcher> allowed = Arrays.stream(allowedNetworks.split(","))
                .map(String::trim)
                .filter(network -> !network.isEmpty())
                .map(IpAddressMatcher::new)
                .toList();
        return http
                .securityMatcher("/internal/mqtt/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().access((authentication, context) -> new AuthorizationDecision(
                                allowed.stream().anyMatch(matcher -> matcher.matches(context.getRequest())))))
                .build();
    }
}
