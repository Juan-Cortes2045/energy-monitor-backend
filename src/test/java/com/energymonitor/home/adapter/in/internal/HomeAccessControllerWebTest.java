package com.energymonitor.home.adapter.in.internal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.home.application.port.in.DescribeHomeAccess;
import com.energymonitor.home.application.port.in.DescribeHomeAccess.HomeAccess;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The home access check the device service asks: who may call it, and what it answers. The token
 * is the one of the test profile; MockMvc requests come from 127.0.0.1.
 */
@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@DisplayName("Home access check")
class HomeAccessControllerWebTest extends JwtKeyedTest {

    private static final String ACCESS = "/internal/homes/hom0000001/access?userId=use0000001";
    private static final String TOKEN = "test-only-internal-token";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DescribeHomeAccess describeHomeAccess;

    @Test
    @DisplayName("200 with what the caller is in the home")
    void answers() throws Exception {
        given(describeHomeAccess.describe("use0000001", "hom0000001"))
                .willReturn(new HomeAccess(true, true, false));

        mvc.perform(get(ACCESS).header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.member").value(true))
                .andExpect(jsonPath("$.owner").value(false));
    }

    @Test
    @DisplayName("a missing home is an answer too")
    void missingHome() throws Exception {
        given(describeHomeAccess.describe("use0000001", "hom0000001"))
                .willReturn(new HomeAccess(false, false, false));

        mvc.perform(get(ACCESS).header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    @DisplayName("refused without the token")
    void withoutToken() throws Exception {
        mvc.perform(get(ACCESS)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("refused with a wrong token")
    void wrongToken() throws Exception {
        mvc.perform(get(ACCESS).header("X-Internal-Token", "guess")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("refused from outside the allowed networks, even with the token")
    void outsideNetwork() throws Exception {
        mvc.perform(get(ACCESS).header("X-Internal-Token", TOKEN).with(request -> {
                    request.setRemoteAddr("203.0.113.7");
                    return request;
                }))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a user's access token does not open it")
    void bearerIsNotEnough() throws Exception {
        mvc.perform(get(ACCESS).header("Authorization", bearer("use0000001")))
                .andExpect(status().isForbidden());
    }
}
