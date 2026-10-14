package com.energymonitor.notification.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.notification.application.usecase.AdminNotifications;
import com.energymonitor.notification.domain.model.Notification;
import com.energymonitor.notification.domain.model.NotificationChannel;
import com.energymonitor.notification.domain.model.NotificationSourceType;
import com.energymonitor.security.api.UserProfile;
import com.energymonitor.security.api.UserProfileQuery;
import com.energymonitor.security.application.command.CheckPermissionQuery;
import com.energymonitor.security.application.port.in.CheckPermission;
import com.energymonitor.security.application.result.AuthenticatedUser;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.UserStatus;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import com.energymonitor.security.infrastructure.JwtTokenIssuer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Route protection and shape of the administrator notification API: {@code USER.READ} is
 * required, and every call works on the token subject's own notifications.
 */
@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class AdminNotificationsControllerWebTest extends JwtKeyedTest {

    private static final String CALLER = "adm0000001";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenIssuer tokenIssuer;

    @MockitoBean
    private CheckPermission checkPermission;

    @MockitoBean
    private AdminNotifications adminNotifications;

    @MockitoBean
    private UserProfileQuery profiles;

    private String bearer() {
        return "Bearer " + tokenIssuer.issue(new AuthenticatedUser(CALLER, "per0000001",
                Email.of("admin@example.com"), UserStatus.ACTIVE, Instant.now(), null));
    }

    private void holdsUserRead(boolean holds) {
        given(checkPermission.check(any())).willAnswer(invocation -> {
            CheckPermissionQuery query = invocation.getArgument(0);
            return holds && query.idUser().equals(CALLER) && query.permissionCode().equals("USER.READ");
        });
    }

    @Test
    void listsTheCallersNotificationsWithTheAffectedAccount() throws Exception {
        holdsUserRead(true);
        Notification n = Notification.pending("not0000001", CALLER, null, NotificationChannel.IN_APP,
                NotificationSourceType.ADMINISTRATION, "use0000009", "admin.accountRegistered");
        n.markAsSent(LocalDateTime.parse("2026-10-14T12:00:00"));
        given(adminNotifications.list(CALLER)).willReturn(List.of(n));
        given(profiles.findByIds(anyCollection())).willReturn(Map.of("use0000009",
                new UserProfile("use0000009", "Ada", "Lovelace", "ada@example.com")));

        mvc.perform(get("/api/v1/admin/notifications").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("not0000001"))
                .andExpect(jsonPath("$[0].messageKey").value("admin.accountRegistered"))
                .andExpect(jsonPath("$[0].subjectName").value("Ada Lovelace"))
                .andExpect(jsonPath("$[0].subjectEmail").value("ada@example.com"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void refusesACallerWithoutUserRead() throws Exception {
        holdsUserRead(false);

        mvc.perform(get("/api/v1/admin/notifications").header("Authorization", bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/notifications/not0000001/read").header("Authorization", bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/admin/notifications/not0000001").header("Authorization", bearer()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminNotifications);
    }

    @Test
    void refusesWithoutAToken() throws Exception {
        mvc.perform(get("/api/v1/admin/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void marksReadAndDeletesForTheTokenSubject() throws Exception {
        holdsUserRead(true);

        mvc.perform(put("/api/v1/admin/notifications/not0000001/read").header("Authorization", bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/admin/notifications/not0000001").header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        verify(adminNotifications).markRead(CALLER, "not0000001");
        verify(adminNotifications).delete(CALLER, "not0000001");
    }

    @Test
    void answersNotFoundForAnotherAdministratorsNotification() throws Exception {
        holdsUserRead(true);
        willThrow(new NoSuchElementException("no")).given(adminNotifications)
                .markRead(CALLER, "not0000002");

        mvc.perform(put("/api/v1/admin/notifications/not0000002/read").header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }
}
