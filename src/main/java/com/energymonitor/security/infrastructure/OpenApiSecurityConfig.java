package com.energymonitor.security.infrastructure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the OpenAPI description and the one security scheme this API authenticates with.
 *
 * <p>The scheme is a stateless bearer token: the access token is a JWT the security service
 * signs, so a client authenticates by sending {@code Authorization: Bearer <token>} and nothing
 * else. This is declared once, here, rather than repeated per endpoint; the endpoints that
 * require it say so with {@code @SecurityRequirement(name = "bearerAuth")}.
 *
 * <p>There is no {@code apiKey} cookie scheme because no credential is ambient: the API creates
 * no HTTP session and sets no cookie.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Energy Monitor API",
        version = "v1",
        description = "Homes, devices, measurements, alerts, recommendations and notifications of "
                + "Energy Monitor. Sign-in, sessions and accounts are served by the security "
                + "service, which issues the bearer token this API validates."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiSecurityConfig {
}