package com.energymonitor.home.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.home.application.port.in.CreateHome;
import com.energymonitor.home.application.port.in.JoinHome;
import com.energymonitor.home.application.port.in.LeaveHome;
import com.energymonitor.home.application.port.in.ListHomes;
import com.energymonitor.home.application.port.in.ListMembers;
import com.energymonitor.home.application.port.in.RemoveUser;
import com.energymonitor.home.application.port.in.ToggleFavorite;
import com.energymonitor.home.infrastructure.ClockConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * What the module's own advice answers when a request cannot be attributed to a user.
 *
 * <p>The advice is not a controller, so a slice has to be pointed at {@link HomeController} to
 * have anything to fail: an exception is raised inside it and mapped afterwards. Every dependency
 * of that controller is mocked except {@link CurrentUserResolver}, which is the real one: the
 * point is the mapping, and the resolver is what raises the exception being mapped.
 *
 * <p>See {@link HomeControllerTest} for why the {@link ClockConfiguration} import and the
 * disabled filters are needed.
 */
@WebMvcTest(HomeController.class)
@Import({ClockConfiguration.class, CurrentUserResolver.class})
@AutoConfigureMockMvc(addFilters = false)
class HomeExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateHome createHome;

    @MockitoBean
    private JoinHome joinHome;

    @MockitoBean
    private ListHomes listHomes;

    @MockitoBean
    private ListMembers listMembers;

    @MockitoBean
    private ToggleFavorite toggleFavorite;

    @MockitoBean
    private RemoveUser removeUser;

    @MockitoBean
    private LeaveHome leaveHome;

    @Test
    void missingUserIdentityReturns401() throws Exception {
        // No token in the security context: the real resolver raises MissingUserIdentityException,
        // and the advice must map it to 401, never to a 500.
        mockMvc.perform(get("/api/v1/homes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * The counterpart: once the caller can be identified the request succeeds and answers an
     * empty list. Named for what it verifies, because a listing cannot report a missing home.
     */
    @Test
    void listHomesReturns200WhenIdentityIsPresent() throws Exception {
        when(listHomes.list(any())).thenReturn(List.of());

        // Filters are off in this class, so nothing would load a jwt() post-processor into the
        // security context; the authenticated JWT is placed there directly instead.
        Jwt token = Jwt.withTokenValue("token").header("alg", "none").subject("use0000001").build();
        TestSecurityContextHolder.setAuthentication(new JwtAuthenticationToken(token));
        try {
            mockMvc.perform(get("/api/v1/homes")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        } finally {
            TestSecurityContextHolder.clearContext();
        }
    }
}
