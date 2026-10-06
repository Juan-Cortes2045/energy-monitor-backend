package com.energymonitor.home.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.home.application.exception.MissingUserIdentityException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * What the module's own advice answers when a request cannot be attributed to a user.
 *
 * <p>The advice is not a controller, so a slice has to be pointed at {@link HomeController} to
 * have anything to fail: an exception is raised inside it and mapped afterwards. Every dependency
 * of that controller is therefore mocked, since the point is the mapping, not the use cases.
 *
 * <p>See {@link HomeControllerTest} for why the {@link ClockConfiguration} import and the
 * disabled filters are needed.
 */
@WebMvcTest(HomeController.class)
@Import(ClockConfiguration.class)
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

    @MockitoBean
    private CurrentUserResolver currentUser;

    @Test
    void missingUserIdentityReturns401() throws Exception {
        when(currentUser.resolveCurrentUserId())
                .thenThrow(new MissingUserIdentityException("No authenticated user in the request."));

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
        when(currentUser.resolveCurrentUserId()).thenReturn("use0000001");
        when(listHomes.list(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/homes")
                        .header("X-User-Id", "use0000001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
