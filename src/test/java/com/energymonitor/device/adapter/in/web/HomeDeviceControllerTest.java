package com.energymonitor.device.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.device.application.command.LinkDeviceCommand;
import com.energymonitor.device.application.command.UnlinkDeviceCommand;
import com.energymonitor.device.application.exception.DeviceAlreadyLinkedException;
import com.energymonitor.device.application.exception.DeviceNotFoundException;
import com.energymonitor.device.application.exception.NotHomeOwnerException;
import com.energymonitor.device.application.port.in.LinkDevice;
import com.energymonitor.device.application.port.in.ListHomeDevices;
import com.energymonitor.device.application.port.in.ReissueDeviceCredentials;
import com.energymonitor.device.application.port.in.UnlinkDevice;
import com.energymonitor.device.application.port.in.UpdateDevice;
import com.energymonitor.device.application.result.HomeDeviceResult;
import com.energymonitor.device.application.result.LinkedDeviceResult;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer of the device endpoints of a home: status codes, response shape and the mapping of
 * the business exceptions of the device module.
 *
 * <p>The OWNER and membership rules live in the use cases and are covered by
 * {@code HomeDeviceServiceTest}; here the use cases are mocked and only the HTTP contract is
 * checked. Filters are disabled and {@code Clock} is mocked for the same reasons as in the other
 * web slices.
 */
@WebMvcTest(HomeDeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeDeviceControllerTest {

    private static final String USER_ID = "use0000001";
    private static final String HOME_ID = "hom0000001";
    private static final String DEVICE_ID = "DEV0000001";
    private static final String BASE = "/api/v1/homes/" + HOME_ID + "/devices";
    private static final String LINK_BODY =
            "{\"deviceCode\":\"ABC123\",\"name\":\" Fridge \",\"applianceTypeId\":\"app0000001\",\"location\":\"Kitchen\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListHomeDevices listHomeDevices;

    @MockitoBean
    private LinkDevice linkDevice;

    @MockitoBean
    private UnlinkDevice unlinkDevice;

    @MockitoBean
    private UpdateDevice updateDevice;

    @MockitoBean
    private ReissueDeviceCredentials reissueCredentials;

    @MockitoBean
    private CurrentUserResolver currentUser;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void callerIsTheOwner() {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);
    }

    private static HomeDeviceResult sampleDevice() {
        return new HomeDeviceResult(DEVICE_ID, "Fridge", "app0000001", "Refrigerator", "Kitchen",
                Instant.parse("2026-10-01T10:00:00Z"), "ABC123", "ONLINE", -58,
                Instant.parse("2026-10-13T20:00:00Z"));
    }

    @Test
    @DisplayName("list answers 200 with the devices of the home")
    void listReturnsDevices() throws Exception {
        // Arrange
        when(listHomeDevices.list(USER_ID, HOME_ID)).thenReturn(List.of(sampleDevice()));

        // Act + Assert
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idDevice").value(DEVICE_ID))
                .andExpect(jsonPath("$[0].status").value("ONLINE"))
                .andExpect(jsonPath("$[0].deviceCode").value("ABC123"));
    }

    @Test
    @DisplayName("link answers 201 with the api key and the broker address, and trims the name")
    void linkReturnsCredentials() throws Exception {
        // Arrange
        when(linkDevice.link(any())).thenReturn(new LinkedDeviceResult(sampleDevice(), "generated-key"));

        // Act
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(LINK_BODY))
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiKey").value("generated-key"))
                .andExpect(jsonPath("$.device.idDevice").value(DEVICE_ID))
                .andExpect(jsonPath("$.broker.port").value(8883));
        ArgumentCaptor<LinkDeviceCommand> captor = ArgumentCaptor.forClass(LinkDeviceCommand.class);
        verify(linkDevice, times(1)).link(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Fridge");
        assertThat(captor.getValue().homeId()).isEqualTo(HOME_ID);
    }

    @Test
    @DisplayName("link answers 400 when the device code is not alphanumeric")
    void linkRejectsInvalidDeviceCode() throws Exception {
        // Arrange
        String body = LINK_BODY.replace("ABC123", "AB-12!");

        // Act + Assert
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(linkDevice, times(0)).link(any());
    }

    @Test
    @DisplayName("link answers 403 when the caller is not the OWNER")
    void linkRefusesNonOwner() throws Exception {
        // Arrange
        when(linkDevice.link(any())).thenThrow(new NotHomeOwnerException(HOME_ID));

        // Act + Assert
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(LINK_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("link answers 409 when the device belongs to another home")
    void linkRefusesDeviceLinkedElsewhere() throws Exception {
        // Arrange
        when(linkDevice.link(any())).thenThrow(new DeviceAlreadyLinkedException("ABC123"));

        // Act + Assert
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(LINK_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("update answers 200 with the edited device")
    void updateReturnsDevice() throws Exception {
        // Arrange
        when(updateDevice.update(any())).thenReturn(sampleDevice());
        String body = "{\"name\":\"Fridge\",\"applianceTypeId\":\"app0000001\",\"location\":\"Kitchen\"}";

        // Act + Assert
        mockMvc.perform(put(BASE + "/" + DEVICE_ID).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fridge"));
    }

    @Test
    @DisplayName("reissue credentials answers 200 with a new api key")
    void reissueReturnsNewKey() throws Exception {
        // Arrange
        when(reissueCredentials.reissue(USER_ID, HOME_ID, DEVICE_ID))
                .thenReturn(new LinkedDeviceResult(sampleDevice(), "new-key"));

        // Act + Assert
        mockMvc.perform(post(BASE + "/" + DEVICE_ID + "/credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").value("new-key"));
    }

    @Test
    @DisplayName("reissue credentials answers 404 when the device does not exist")
    void reissueRefusesUnknownDevice() throws Exception {
        // Arrange
        when(reissueCredentials.reissue(eq(USER_ID), eq(HOME_ID), any()))
                .thenThrow(new DeviceNotFoundException("DEV9999999"));

        // Act + Assert
        mockMvc.perform(post(BASE + "/DEV9999999/credentials"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("unlink answers 204 and passes the caller, the home and the device")
    void unlinkReturnsNoContent() throws Exception {
        // Act + Assert
        mockMvc.perform(delete(BASE + "/" + DEVICE_ID))
                .andExpect(status().isNoContent());
        ArgumentCaptor<UnlinkDeviceCommand> captor = ArgumentCaptor.forClass(UnlinkDeviceCommand.class);
        verify(unlinkDevice, times(1)).unlink(captor.capture());
        assertThat(captor.getValue().deviceId()).isEqualTo(DEVICE_ID);
    }

    @Test
    @DisplayName("unlink answers 403 when the caller is not the OWNER")
    void unlinkRefusesNonOwner() throws Exception {
        // Arrange
        doThrow(new NotHomeOwnerException(HOME_ID)).when(unlinkDevice).unlink(any());

        // Act + Assert
        mockMvc.perform(delete(BASE + "/" + DEVICE_ID))
                .andExpect(status().isForbidden());
    }
}
