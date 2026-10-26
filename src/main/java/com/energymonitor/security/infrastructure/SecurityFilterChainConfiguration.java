package com.energymonitor.security.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The HTTP security rules of this backend's API.
 *
 * <p>The security service issues the access tokens and owns sign-in, sessions and accounts; this
 * backend only validates the tokens and reads the caller from their subject. The shape is a
 * stateless bearer-token API:
 *
 * <ul>
 *   <li>No HTTP session is ever created, so nothing about a caller is kept server side and
 *       the API scales horizontally without sticky sessions.</li>
 *   <li>CSRF protection is off, which is correct precisely because there is no session and no
 *       cookie for a cross-site request to ride on: the access token travels in a header the
 *       client sets, never attached automatically by the browser.</li>
 *   <li>Form login and HTTP Basic are off, so the only way in is a bearer token.</li>
 *   <li>JWT signature (the security service's public key), issuer and expiry are validated by
 *       the resource server filter before a request reaches any controller.</li>
 * </ul>
 *
 * <p>Everything else is denied by default: a new endpoint is protected unless it is added to
 * the permit list on purpose.
 *
 * <h2>Administration</h2>
 *
 * <p>This backend serves no admin route: the directory of users and roles is the security
 * service's, and the administrator notifications are the notification service's. Every
 * {@code /api/v1/admin/**} reaching it is denied outright.
 *
 * <h2>CORS</h2>
 *
 * <p>{@code cors(Customizer.withDefaults())} below resolves to the
 * {@code CorsConfigurationSource} bean published by {@link SecurityCorsConfiguration}. That bean
 * exists, so this is a real policy rather than the same-origin fallback Spring Security would
 * otherwise substitute: the server emits CORS headers for the origins the deployment names and
 * for no others.
 *
 * <p>It matters to the React web frontend, which is served from a different origin and would
 * otherwise be blocked by the browser. It does not affect React Native, because CORS is a browser
 * enforcement mechanism and a native HTTP client never applies it.
 *
 * <p>Which origins are allowed, whether credentials may ride along and why no wildcard exists
 * are argued in {@link SecurityCorsConfiguration}. The short version: the origins come from
 * configuration, they default to none, and credentials are off.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityFilterChainConfiguration {

    /**
     * Unauthenticated access to the API description and the health probe, so a container
     * platform can still check liveness.
     */
    private static final String[] INFRASTRUCTURE_ENDPOINTS = {
        "/actuator/health",
        "/actuator/health/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    /**
     * Builds the filter chain protecting the API.
     *
     * @param http            the builder Spring Security hands over
     * @param entryPoint      renders 401 in the shared error shape
     * @param accessDenied    renders 403 in the shared error shape
     * @return the installed filter chain
     * @throws Exception if the chain cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtAuthenticationEntryPoint entryPoint,
                                                    JwtAccessDeniedHandler accessDenied)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                // Resolves to the CorsConfigurationSource published by
                // SecurityCorsConfiguration: the origins named in configuration, no wildcard,
                // and no credentials. Without that bean this call would silently fall back to
                // same-origin-only and emit no CORS header at all.
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDenied))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(INFRASTRUCTURE_ENDPOINTS).permitAll()
                        .requestMatchers("/api/v1/admin/**").denyAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDenied)
                        .jwt(Customizer.withDefaults()))
                .build();
    }
}
