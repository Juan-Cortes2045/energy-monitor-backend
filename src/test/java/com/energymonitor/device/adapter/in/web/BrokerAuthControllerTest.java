package com.energymonitor.device.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.device.application.port.in.AuthorizeBrokerAccess;
import com.energymonitor.device.application.port.in.AuthorizeBrokerAccess.Access;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer of the endpoints Mosquitto calls to authenticate a client and authorize its topics.
 *
 * <p>The broker plugin reads only the status: 200 grants, anything else denies. Filters are
 * disabled for the same reason as in the other web slices; the network restriction of
 * {@code BrokerAuthSecurityConfiguration} is outside the scope of this class. {@code Clock} is
 * mocked because the slice also instantiates the security module's exception handler.
 */
@WebMvcTest(BrokerAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class BrokerAuthControllerTest {

    private static final String USER_URL = "/internal/mqtt/auth/user";
    private static final String ACL_URL = "/internal/mqtt/auth/acl";
    private static final String TOPIC = "energy-monitor/devices/DEV0000001/telemetry";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorizeBrokerAccess authorizeBrokerAccess;

    @MockitoBean
    private Clock clock;

    private static String userBody() {
        return "{\"username\":\"ABC123\",\"password\":\"secret-test-value\",\"clientid\":\"esp32-DEV0000001\"}";
    }

    private static String aclBody(Integer acc) {
        String access = acc == null ? "" : ",\"acc\":" + acc;
        return "{\"username\":\"ABC123\",\"clientid\":\"esp32-DEV0000001\",\"topic\":\"" + TOPIC + "\"" + access + "}";
    }

    @Test
    @DisplayName("authenticate answers 200 when the credentials are valid")
    void authenticateGrantsValidCredentials() throws Exception {
        // Arrange
        when(authorizeBrokerAccess.authenticate("ABC123", "secret-test-value")).thenReturn(true);

        // Act + Assert
        mockMvc.perform(post(USER_URL).contentType(MediaType.APPLICATION_JSON).content(userBody()))
                .andExpect(status().isOk());
        verify(authorizeBrokerAccess, times(1)).authenticate("ABC123", "secret-test-value");
    }

    @Test
    @DisplayName("authenticate answers 403 when the credentials are not valid")
    void authenticateRefusesInvalidCredentials() throws Exception {
        // Arrange
        when(authorizeBrokerAccess.authenticate(any(), any())).thenReturn(false);

        // Act + Assert
        mockMvc.perform(post(USER_URL).contentType(MediaType.APPLICATION_JSON).content(userBody()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "acc={0} is checked as {1}")
    @CsvSource({"1,READ", "2,WRITE", "4,SUBSCRIBE"})
    @DisplayName("acl maps the broker access code to the matching access and grants it")
    void aclGrantsSingleAccess(int acc, Access expected) throws Exception {
        // Arrange
        when(authorizeBrokerAccess.authorize("ABC123", TOPIC, expected)).thenReturn(true);

        // Act + Assert
        mockMvc.perform(post(ACL_URL).contentType(MediaType.APPLICATION_JSON).content(aclBody(acc)))
                .andExpect(status().isOk());
        verify(authorizeBrokerAccess, times(1)).authorize("ABC123", TOPIC, expected);
    }

    @Test
    @DisplayName("acl answers 403 when the topic is not allowed for the client")
    void aclRefusesForeignTopic() throws Exception {
        // Arrange
        when(authorizeBrokerAccess.authorize(any(), any(), any())).thenReturn(false);

        // Act + Assert
        mockMvc.perform(post(ACL_URL).contentType(MediaType.APPLICATION_JSON).content(aclBody(2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("acl with read and write requires both accesses")
    void aclReadWriteRequiresBoth() throws Exception {
        // Arrange
        when(authorizeBrokerAccess.authorize("ABC123", TOPIC, Access.READ)).thenReturn(false);
        when(authorizeBrokerAccess.authorize("ABC123", TOPIC, Access.WRITE)).thenReturn(true);

        // Act + Assert
        mockMvc.perform(post(ACL_URL).contentType(MediaType.APPLICATION_JSON).content(aclBody(3)))
                .andExpect(status().isForbidden());
        verify(authorizeBrokerAccess, never()).authorize(eq("ABC123"), eq(TOPIC), eq(Access.WRITE));
    }

    @Test
    @DisplayName("acl with read and write grants when both accesses are allowed")
    void aclReadWriteGrantsWhenBothAllowed() throws Exception {
        // Arrange
        when(authorizeBrokerAccess.authorize(any(), any(), any())).thenReturn(true);

        // Act + Assert
        mockMvc.perform(post(ACL_URL).contentType(MediaType.APPLICATION_JSON).content(aclBody(3)))
                .andExpect(status().isOk());
        verify(authorizeBrokerAccess, times(1)).authorize("ABC123", TOPIC, Access.READ);
        verify(authorizeBrokerAccess, times(1)).authorize("ABC123", TOPIC, Access.WRITE);
    }

    @ParameterizedTest(name = "acc={0} is refused without asking the use case")
    @CsvSource({"0", "5", "-1"})
    @DisplayName("acl refuses an unknown access code")
    void aclRefusesUnknownAccessCode(int acc) throws Exception {
        // Act + Assert
        mockMvc.perform(post(ACL_URL).contentType(MediaType.APPLICATION_JSON).content(aclBody(acc)))
                .andExpect(status().isForbidden());
        verify(authorizeBrokerAccess, never()).authorize(any(), any(), any());
    }

    @Test
    @DisplayName("acl refuses a request without an access code")
    void aclRefusesMissingAccessCode() throws Exception {
        // Act + Assert
        mockMvc.perform(post(ACL_URL).contentType(MediaType.APPLICATION_JSON).content(aclBody(null)))
                .andExpect(status().isForbidden());
        verify(authorizeBrokerAccess, never()).authorize(any(), any(), any());
    }
}
