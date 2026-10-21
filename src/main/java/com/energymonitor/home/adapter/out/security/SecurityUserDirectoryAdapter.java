package com.energymonitor.home.adapter.out.security;

import com.energymonitor.home.application.port.out.UserDirectoryPort;
import com.energymonitor.security.api.SecurityServiceUnavailableException;
import com.energymonitor.security.api.UserProfile;
import com.energymonitor.security.api.UserProfileQuery;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads user names and addresses from the security service, through {@code security::api}.
 *
 * <p>When the security service does not answer, the lists of members and homes are still shown,
 * only without names and addresses.
 */
@Component
public class SecurityUserDirectoryAdapter implements UserDirectoryPort {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityUserDirectoryAdapter.class);

    private final UserProfileQuery profiles;

    public SecurityUserDirectoryAdapter(UserProfileQuery profiles) {
        this.profiles = profiles;
    }

    @Override
    public Map<String, UserSummary> findByIds(Collection<String> userIds) {
        Map<String, UserProfile> found;
        try {
            found = profiles.findByIds(userIds);
        } catch (SecurityServiceUnavailableException unavailable) {
            LOG.warn("Showing accounts without names: {}", unavailable.getMessage());
            return Map.of();
        }
        return found.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new UserSummary(
                        entry.getValue().name(), entry.getValue().lastName(),
                        entry.getValue().email())));
    }
}
