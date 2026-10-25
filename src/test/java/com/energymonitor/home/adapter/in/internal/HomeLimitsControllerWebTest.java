package com.energymonitor.home.adapter.in.internal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.home.api.LimitPeriod;
import com.energymonitor.home.application.port.in.DescribeHomeLimits;
import com.energymonitor.home.application.port.in.DescribeHomeLimits.HomeLimits;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The home limits the measurement service asks: who may call it, and what it answers. The token
 * is the one of the test profile; MockMvc requests come from 127.0.0.1.
 */
@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@DisplayName("Home limits for the measurement service")
class HomeLimitsControllerWebTest extends JwtKeyedTest {

    private static final String LIMITS = "/internal/homes/hom0000001/thresholds";
    private static final String TOKEN = "test-only-internal-token";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DescribeHomeLimits describeHomeLimits;

    @Test
    @DisplayName("200 with the limits of the home")
    void answers() throws Exception {
        given(describeHomeLimits.describe("hom0000001"))
                .willReturn(Optional.of(new HomeLimits(10.0, 300.0, LimitPeriod.MONTHLY)));

        mvc.perform(get(LIMITS).header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLimit").value(10.0))
                .andExpect(jsonPath("$.monthlyLimit").value(300.0))
                .andExpect(jsonPath("$.limitPeriod").value("MONTHLY"));
    }

    @Test
    @DisplayName("404 when the home has none")
    void none() throws Exception {
        given(describeHomeLimits.describe("hom0000001")).willReturn(Optional.empty());

        mvc.perform(get(LIMITS).header("X-Internal-Token", TOKEN)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("refused without the token, with a wrong one or from outside the allowed networks")
    void refused() throws Exception {
        mvc.perform(get(LIMITS)).andExpect(status().isForbidden());
        mvc.perform(get(LIMITS).header("X-Internal-Token", "guess")).andExpect(status().isForbidden());
        mvc.perform(get(LIMITS).header("X-Internal-Token", TOKEN).with(request -> {
                    request.setRemoteAddr("203.0.113.7");
                    return request;
                }))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a user's access token does not open it")
    void bearerIsNotEnough() throws Exception {
        mvc.perform(get(LIMITS).header("Authorization", bearer("use0000001")))
                .andExpect(status().isForbidden());
    }
}
