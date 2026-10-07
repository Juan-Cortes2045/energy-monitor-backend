package com.energymonitor.home.adapter.out.security;

import com.energymonitor.home.application.port.out.UserDirectoryPort;
import com.energymonitor.security.api.UserProfileQuery;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Reads user names and addresses from the security module's public API.
 */
@Component
public class SecurityUserDirectoryAdapter implements UserDirectoryPort {

    private final UserProfileQuery profiles;

    public SecurityUserDirectoryAdapter(UserProfileQuery profiles) {
        this.profiles = profiles;
    }

    @Override
    public Map<String, UserSummary> findByIds(Collection<String> userIds) {
        return profiles.findByIds(userIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new UserSummary(
                        entry.getValue().name(), entry.getValue().lastName(),
                        entry.getValue().email())));
    }
}
