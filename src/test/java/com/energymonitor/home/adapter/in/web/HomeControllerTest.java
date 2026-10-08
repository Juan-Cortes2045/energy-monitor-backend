package com.energymonitor.home.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import com.energymonitor.home.application.command.ListMembersQuery;
import com.energymonitor.home.application.command.RemoveUserCommand;
import com.energymonitor.home.application.command.ToggleFavoriteCommand;
import com.energymonitor.home.application.exception.AccessCodeNotFoundException;
import com.energymonitor.home.application.exception.HomeConflictException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import com.energymonitor.home.application.port.in.CreateHome;
import com.energymonitor.home.application.port.in.JoinHome;
import com.energymonitor.home.application.port.in.LeaveHome;
import com.energymonitor.home.application.port.in.ListHomes;
import com.energymonitor.home.application.port.in.ListMembers;
import com.energymonitor.home.application.port.in.RemoveUser;
import com.energymonitor.home.application.port.in.ToggleFavorite;
import com.energymonitor.home.application.result.HomeMembershipResult;
import com.energymonitor.home.application.result.HomeResult;
import com.energymonitor.home.application.result.UserHomeResult;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.infrastructure.ClockConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * A slice loads {@code @ControllerAdvice} beans but no {@code @Configuration}, so the
 * {@link java.time.Clock} every advice constructor asks for is absent. Importing the module's
 * own {@link ClockConfiguration} supplies it from production wiring instead of a test-only
 * bean, which keeps the two definitions from drifting apart.
 *
 * <p>Filters stay on. The slice excludes {@code SecurityFilterChainConfiguration}, so the chain in
 * place is Spring Boot's default one: it demands an authenticated caller and a CSRF token on every
 * write. Each request therefore carries {@code jwt()}, which hands the chain an authenticated JWT
 * whose subject is the caller, and {@code csrf()}. The filters are what load that JWT into the
 * security context, which is why they cannot be switched off here. The real
 * {@link CurrentUserResolver} is imported rather than mocked, so the caller is resolved from the
 * token's subject exactly as in production. The production chain and a real RS256 token are
 * covered by {@code HomeCallerIdentityIntegrationTest}.
 */
@WebMvcTest(HomeController.class)
@Import({ClockConfiguration.class, CurrentUserResolver.class})
@AutoConfigureMockMvc
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
    private ListMembers listMembers;

    @MockitoBean
    private ToggleFavorite toggleFavorite;

    @MockitoBean
    private RemoveUser removeUser;

    @MockitoBean
    private LeaveHome leaveHome;

    private static final String USER_ID = "use0000001";
    private static final String HOME_ID = "hom0000001";
    private static final String OTHER_USER_ID = "use0000009";

    /**
     * A request authenticated as {@code userId}: an access token whose subject is that account,
     * plus the CSRF token the slice's default chain demands on writes.
     */
    private static RequestPostProcessor asUser(String userId) {
        RequestPostProcessor token = jwt().jwt(jwt -> jwt.subject(userId));
        return request -> csrf().postProcessRequest(token.postProcessRequest(request));
    }

    @Test
    void createHomeReturns201() throws Exception {
        when(createHome.create(any(CreateHomeCommand.class)))
                .thenReturn(new HomeResult(HOME_ID, "Casa", "hous000001", "Calle 123", "ABC12345", "Mi casa", Instant.now()));

        mockMvc.perform(post("/api/v1/homes")
                        .with(asUser(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Casa\",\"homeTypeId\":\"hous000001\",\"address\":\"Calle 123\",\"description\":\"Mi casa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idHome").value(HOME_ID))
                .andExpect(jsonPath("$.name").value("Casa"));
    }

    @Test
    void createHomeWithoutTokenReturns401() throws Exception {
        // The CSRF token is present so the refusal is about the missing caller, not about CSRF.
        mockMvc.perform(post("/api/v1/homes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Casa\",\"homeTypeId\":\"hous000001\",\"address\":\"Calle 123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createHomeWithInvalidDataReturns400() throws Exception {

        mockMvc.perform(post("/api/v1/homes")
                        .with(asUser(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"homeTypeId\":\"\",\"address\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listHomesReturns200() throws Exception {
        when(listHomes.list(any(ListHomesQuery.class)))
                .thenReturn(List.of(new HomeMembershipResult(HOME_ID, "Casa", "hous000001", "Calle 123", "ABC12345", "Mi casa", Instant.now(), Role.OWNER, false, "Ada", "Lovelace", "ada@example.com")));

        mockMvc.perform(get("/api/v1/homes")
                        .with(asUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idHome").value(HOME_ID))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[0].userResponsible").value("Ada"))
                .andExpect(jsonPath("$[0].userResponsibleLastName").value("Lovelace"))
                .andExpect(jsonPath("$[0].userResponsibleEmail").value("ada@example.com"));
    }

    @Test
    void listMembersReturns200() throws Exception {
        when(listMembers.list(any(ListMembersQuery.class)))
                .thenReturn(List.of(new UserHomeResult("use0000002", HOME_ID, Role.MEMBER, false, "Grace", "Hopper", "grace@example.com")));

        mockMvc.perform(get("/api/v1/homes/" + HOME_ID + "/members")
                        .with(asUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("use0000002"))
                .andExpect(jsonPath("$[0].role").value("MEMBER"))
                .andExpect(jsonPath("$[0].name").value("Grace"))
                .andExpect(jsonPath("$[0].lastName").value("Hopper"))
                .andExpect(jsonPath("$[0].email").value("grace@example.com"));
    }

    @Test
    void listMembersReturns404WhenNotMember() throws Exception {
        when(listMembers.list(any(ListMembersQuery.class)))
                .thenThrow(new HomeNotFoundException("no active membership"));

        mockMvc.perform(get("/api/v1/homes/" + HOME_ID + "/members")
                        .with(asUser(USER_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinHomeReturns201() throws Exception {
        when(joinHome.join(any(JoinHomeCommand.class)))
                .thenReturn(new UserHomeResult(USER_ID, HOME_ID, Role.MEMBER, false, null, null, null));

        mockMvc.perform(post("/api/v1/homes/join")
                        .with(asUser(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessCode\":\"ABC12345\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void joinHomeWithInvalidCodeReturns404() throws Exception {
        when(joinHome.join(any(JoinHomeCommand.class)))
                .thenThrow(new AccessCodeNotFoundException("no active home with access code"));

        mockMvc.perform(post("/api/v1/homes/join")
                        .with(asUser(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessCode\":\"WRONG123\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleFavoriteReturns200() throws Exception {
        when(toggleFavorite.toggle(any(ToggleFavoriteCommand.class)))
                .thenReturn(new UserHomeResult(USER_ID, HOME_ID, Role.OWNER, true, null, null, null));

        mockMvc.perform(put("/api/v1/homes/{homeId}/favorite", HOME_ID)
                        .with(asUser(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true));
    }

    @Test
    void toggleFavoriteWithoutMembershipReturns404() throws Exception {
        when(toggleFavorite.toggle(any(ToggleFavoriteCommand.class)))
                .thenThrow(new UserHomeNotFoundException("no active membership"));

        mockMvc.perform(put("/api/v1/homes/{homeId}/favorite", HOME_ID)
                        .with(asUser(USER_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeUserReturns204() throws Exception {

        mockMvc.perform(delete("/api/v1/homes/{homeId}/members/{userId}", HOME_ID, "use0000002")
                        .with(asUser(USER_ID)))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeUserWithoutMembershipReturns404() throws Exception {
        org.mockito.Mockito.doThrow(new HomeNotFoundException("no active membership"))
                .when(removeUser).remove(any(RemoveUserCommand.class));

        mockMvc.perform(delete("/api/v1/homes/{homeId}/members/{userId}", HOME_ID, "use0000002")
                        .with(asUser(USER_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void leaveHomeReturns204() throws Exception {

        mockMvc.perform(delete("/api/v1/homes/{homeId}/members/me", HOME_ID)
                        .with(asUser(USER_ID)))
                .andExpect(status().isNoContent());
    }

    @Test
    void leaveHomeWithoutMembershipReturns404() throws Exception {
        org.mockito.Mockito.doThrow(new UserHomeNotFoundException("no active membership"))
                .when(leaveHome).leave(any(LeaveHomeCommand.class));

        mockMvc.perform(delete("/api/v1/homes/{homeId}/members/me", HOME_ID)
                        .with(asUser(USER_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ignoresAnXUserIdHeaderThatNamesSomeoneElse() throws Exception {
        // The token says USER_ID; the header claims OTHER_USER_ID. Only the token is evidence, so
        // the listing must be the token holder's, never the account the header names.
        when(listHomes.list(any(ListHomesQuery.class))).thenAnswer(invocation -> {
            ListHomesQuery query = invocation.getArgument(0);
            String homeId = USER_ID.equals(query.userId()) ? HOME_ID : "hom0000009";
            return List.of(new HomeMembershipResult(homeId, "Casa", "hous000001", "Calle 123",
                    "ABC12345", null, Instant.now(), Role.OWNER, false, "Ada", "Lovelace",
                    "ada@example.com"));
        });

        mockMvc.perform(get("/api/v1/homes")
                        .with(asUser(USER_ID))
                        .header("X-User-Id", OTHER_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idHome").value(HOME_ID));

        verify(listHomes).list(new ListHomesQuery(USER_ID));
    }

    @Test
    void anXUserIdHeaderAloneDoesNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/v1/homes").header("X-User-Id", USER_ID))
                .andExpect(status().isUnauthorized());
    }
}
