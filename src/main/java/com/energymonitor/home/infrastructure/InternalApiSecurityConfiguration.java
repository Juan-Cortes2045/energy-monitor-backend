package com.energymonitor.home.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Security of the endpoints the other services call: the deletion check of the security service,
 * the home access check of the device and measurement services, and the home limits the
 * measurement service shows.
 *
 * <p>Those services present no JWT, so these paths get their own filter chain, evaluated
 * before the API chain. The caller must come from {@code internal-api.allowed-networks}
 * <em>and</em> send {@code internal-api.token} in {@code X-Internal-Token}: any container on the
 * Docker network passes the first check, so it is not enough alone. With no token configured
 * every call is refused.
 *
 * <p>Behind a reverse proxy every request seems to come from the proxy, so the proxy must not
 * forward {@code /internal/**} at all.
 */
@Configuration(proxyBeanMethods = false)
public class InternalApiSecurityConfiguration {

    static final String TOKEN_HEADER = "X-Internal-Token";

    private static final Logger LOG = LoggerFactory.getLogger(InternalApiSecurityConfiguration.class);

    @Bean
    @Order(2)
    public SecurityFilterChain internalApiFilterChain(
            HttpSecurity http,
            @Value("${internal-api.token:}") String token,
            @Value("${internal-api.allowed-networks:127.0.0.1/32,::1/128,172.16.0.0/12}") String allowedNetworks)
            throws Exception {
        if (token.isBlank()) {
            LOG.warn("internal-api.token is empty: every call to /internal/accounts/** and"
                    + " /internal/homes/** will be refused");
        }
        byte[] expected = token.getBytes(StandardCharsets.UTF_8);
        List<IpAddressMatcher> allowed = Arrays.stream(allowedNetworks.split(","))
                .map(String::trim)
                .filter(network -> !network.isEmpty())
                .map(IpAddressMatcher::new)
                .toList();
        return http
                .securityMatcher("/internal/accounts/**", "/internal/homes/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().access((authentication, context) -> new AuthorizationDecision(
                                trusted(context.getRequest(), expected, allowed))))
                .build();
    }

    private static boolean trusted(HttpServletRequest request, byte[] expected,
                                   List<IpAddressMatcher> allowed) {
        String presented = request.getHeader(TOKEN_HEADER);
        return expected.length > 0
                && presented != null
                && allowed.stream().anyMatch(network -> network.matches(request))
                // Constant time, so the answer time says nothing about how much of it matched.
                && MessageDigest.isEqual(expected, presented.getBytes(StandardCharsets.UTF_8));
    }
}
