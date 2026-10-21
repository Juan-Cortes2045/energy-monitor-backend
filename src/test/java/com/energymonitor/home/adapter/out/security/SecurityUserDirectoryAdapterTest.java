package com.energymonitor.home.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.energymonitor.security.api.SecurityServiceUnavailableException;
import com.energymonitor.security.api.UserProfile;
import com.energymonitor.security.api.UserProfileQuery;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User directory over the security API")
class SecurityUserDirectoryAdapterTest {

    private final UserProfileQuery profiles = mock(UserProfileQuery.class);
    private final SecurityUserDirectoryAdapter adapter = new SecurityUserDirectoryAdapter(profiles);

    @Test
    @DisplayName("maps the profiles to names and addresses")
    void maps() {
        given(profiles.findByIds(List.of("use0000001"))).willReturn(Map.of("use0000001",
                new UserProfile("use0000001", "Ada", "Lovelace", "ada@example.com")));

        assertThat(adapter.findByIds(List.of("use0000001"))).hasEntrySatisfying("use0000001",
                summary -> assertThat(summary.email()).isEqualTo("ada@example.com"));
    }

    @Test
    @DisplayName("without the security service the accounts come without names")
    void degrades() {
        given(profiles.findByIds(List.of("use0000001")))
                .willThrow(new SecurityServiceUnavailableException("down", null));

        assertThat(adapter.findByIds(List.of("use0000001"))).isEmpty();
    }
}
