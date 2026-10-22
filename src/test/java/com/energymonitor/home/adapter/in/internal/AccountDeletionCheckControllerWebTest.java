package com.energymonitor.home.adapter.in.internal;

import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.home.application.port.in.CheckAccountDeletion;
import com.energymonitor.home.application.exception.AccountDeletionBlockedException;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The deletion check the security service asks: who may call it, and what it answers. The token
 * is the one of the test profile; MockMvc requests come from 127.0.0.1.
 */
@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@DisplayName("Account deletion check")
class AccountDeletionCheckControllerWebTest extends JwtKeyedTest {

    private static final String CHECK = "/internal/accounts/use0000001/deletion-check";
    private static final String TOKEN = "test-only-internal-token";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CheckAccountDeletion checkAccountDeletion;

    @Test
    @DisplayName("204 when the account may be deleted")
    void allowed() throws Exception {
        mvc.perform(get(CHECK).header("X-Internal-Token", TOKEN)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("409 with the message for the user when it may not")
    void blocked() throws Exception {
        willThrow(new AccountDeletionBlockedException("Transfer the ownership first."))
                .given(checkAccountDeletion).ensureDeletable("use0000001");

        mvc.perform(get(CHECK).header("X-Internal-Token", TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Transfer the ownership first."));
    }

    @Test
    @DisplayName("refused without the token")
    void withoutToken() throws Exception {
        mvc.perform(get(CHECK)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("refused with a wrong token")
    void wrongToken() throws Exception {
        mvc.perform(get(CHECK).header("X-Internal-Token", "guess")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("refused from outside the allowed networks, even with the token")
    void outsideNetwork() throws Exception {
        mvc.perform(get(CHECK).header("X-Internal-Token", TOKEN).with(request -> {
                    request.setRemoteAddr("203.0.113.7");
                    return request;
                }))
                .andExpect(status().isForbidden());
    }
}
