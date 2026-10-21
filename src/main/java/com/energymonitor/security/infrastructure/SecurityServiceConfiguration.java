package com.energymonitor.security.infrastructure;

import com.energymonitor.security.adapter.out.remote.RemoteAdministratorQuery;
import com.energymonitor.security.adapter.out.remote.RemotePermissionCheck;
import com.energymonitor.security.adapter.out.remote.RemoteUserProfileQuery;
import com.energymonitor.security.adapter.out.remote.SecurityServiceClient;
import com.energymonitor.security.api.AdministratorQuery;
import com.energymonitor.security.api.UserProfileQuery;
import com.github.benmanes.caffeine.cache.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Connects this backend to the security service: the permissions the admin routes require, the
 * profiles other modules show and the administrators the notifications go to, each answered by
 * its internal API and cached for a few seconds.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityServiceProperties.class)
public class SecurityServiceConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityServiceConfiguration.class);

    @Bean
    public SecurityServiceClient securityServiceClient(SecurityServiceProperties properties,
                                                       @Value("${internal-api.token:}") String token) {
        if (token == null || token.isBlank()) {
            LOG.warn("internal-api.token is empty: the security service refuses every call, so the "
                    + "admin routes answer 503 and no profile or administrator can be read");
        }
        return SecurityServiceClient.create(properties.url().toString(), token,
                properties.connectTimeout(), properties.readTimeout());
    }

    @Bean
    public RemotePermissionCheck remotePermissionCheck(SecurityServiceClient client,
                                                       SecurityServiceProperties properties) {
        return new RemotePermissionCheck(client, properties.permissionsTtl(), Ticker.systemTicker());
    }

    @Bean
    public UserProfileQuery userProfileQuery(SecurityServiceClient client,
                                             SecurityServiceProperties properties) {
        return new RemoteUserProfileQuery(client, properties.profilesTtl(), Ticker.systemTicker());
    }

    @Bean
    public AdministratorQuery administratorQuery(SecurityServiceClient client,
                                                 SecurityServiceProperties properties) {
        return new RemoteAdministratorQuery(client, properties.administratorsTtl(),
                Ticker.systemTicker());
    }
}
