package com.energymonitor.home.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.home.application.command.CreateHomeCommand;
import com.energymonitor.home.application.command.JoinHomeCommand;
import com.energymonitor.home.application.command.LeaveHomeCommand;
import com.energymonitor.home.application.command.ListHomesQuery;
import com.energymonitor.home.application.command.RemoveUserCommand;
import com.energymonitor.home.application.command.ToggleFavoriteCommand;
import com.energymonitor.home.application.exception.AccessCodeNotFoundException;
import com.energymonitor.home.application.exception.HomeConflictException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.exception.MissingUserIdentityException;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import com.energymonitor.home.application.port.in.CreateHome;
import com.energymonitor.home.application.port.in.JoinHome;
import com.energymonitor.home.application.port.in.LeaveHome;
import com.energymonitor.home.application.port.in.ListHomes;
import com.energymonitor.home.application.port.in.RemoveUser;
import com.energymonitor.home.application.port.in.ToggleFavorite;
import com.energymonitor.home.application.result.HomeMembershipResult;
import com.energymonitor.home.application.result.HomeResult;
import com.energymonitor.home.application.result.UserHomeResult;
import com.energymonitor.home.domain.model.Role;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateHome createHome;

    @MockitoBean
    private JoinHome joinHome;

    @MockitoBean
    private ListHomes listHomes;

    @MockitoBean
    private ToggleFavorite toggleFavorite;

    @MockitoBean
    private RemoveUser removeUser;

    @MockitoBean
    private LeaveHome leaveHome;

    @MockitoBean
    private CurrentUserResolver currentUser;

    private static final String USER_ID = "use0000001";
    private static final String HOME_ID = "hom0000001";

    @Test
    void createHomeReturns201() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);
        when(createHome.create(any(CreateHomeCommand.class)))
                .thenReturn(new HomeResult(HOME_ID, "Casa", "hous000001", "Calle 123", "ABC12345", "Mi casa", Instant.now()));

        mockMvc.perform(post("/api/v1/homes")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Casa\",\"homeTypeId\":\"hous000001\",\"address\":\"Calle 123\",\"description\":\"Mi casa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idHome").value(HOME_ID))
                .andExpect(jsonPath("$.name").value("Casa"));
    }

    @Test
    void createHomeWithoutHeaderReturns401() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenThrow(new MissingUserIdentityException("missing X-User-Id header"));

        mockMvc.perform(post("/api/v1/homes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Casa\",\"homeTypeId\":\"hous000001\",\"address\":\"Calle 123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createHomeWithInvalidDataReturns400() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);

        mockMvc.perform(post("/api/v1/homes")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"homeTypeId\":\"\",\"address\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listHomesReturns200() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);
        when(listHomes.list(any(ListHomesQuery.class)))
                .thenReturn(List.of(new HomeMembershipResult(HOME_ID, "Casa", "hous000001", "Calle 123", "ABC12345", "Mi casa", Instant.now(), Role.OWNER, false)));

        mockMvc.perform(get("/api/v1/homes")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idHome").value(HOME_ID))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void joinHomeReturns201() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);
        when(joinHome.join(any(JoinHomeCommand.class)))
                .thenReturn(new UserHomeResult(USER_ID, HOME_ID, Role.MEMBER, false));

        mockMvc.perform(post("/api/v1/homes/join")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessCode\":\"ABC12345\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void joinHomeWithInvalidCodeReturns404() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);
        when(joinHome.join(any(JoinHomeCommand.class)))
                .thenThrow(new AccessCodeNotFoundException("no active home with access code"));

        mockMvc.perform(post("/api/v1/homes/join")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessCode\":\"WRONG123\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleFavoriteReturns200() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);
        when(toggleFavorite.toggle(any(ToggleFavoriteCommand.class)))
                .thenReturn(new UserHomeResult(USER_ID, HOME_ID, Role.OWNER, true));

        mockMvc.perform(put("/api/v1/homes/{homeId}/favorite", HOME_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true));
    }

    @Test
    void toggleFavoriteWithoutMembershipReturns404() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);
        when(toggleFavorite.toggle(any(ToggleFavoriteCommand.class)))
                .thenThrow(new UserHomeNotFoundException("no active membership"));

        mockMvc.perform(put("/api/v1/homes/{homeId}/favorite", HOME_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeUserReturns204() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);

        mockMvc.perform(delete("/api/v1/homes/{homeId}/members/{userId}", HOME_ID, "use0000002")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeUserWithoutMembershipReturns404() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);
        org.mockito.Mockito.doThrow(new HomeNotFoundException("no active membership"))
                .when(removeUser).remove(any(RemoveUserCommand.class));

        mockMvc.perform(delete("/api/v1/homes/{homeId}/members/{userId}", HOME_ID, "use0000002")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void leaveHomeReturns204() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);

        mockMvc.perform(delete("/api/v1/homes/{homeId}/members/me", HOME_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void leaveHomeWithoutMembershipReturns404() throws Exception {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);
        org.mockito.Mockito.doThrow(new UserHomeNotFoundException("no active membership"))
                .when(leaveHome).leave(any(LeaveHomeCommand.class));

        mockMvc.perform(delete("/api/v1/homes/{homeId}/members/me", HOME_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }
}
