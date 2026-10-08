package com.energymonitor.device.adapter.in.web;

import com.energymonitor.device.adapter.in.web.dto.HomeDeviceResponse;
import com.energymonitor.device.adapter.in.web.dto.LinkDeviceRequest;
import com.energymonitor.device.adapter.in.web.dto.LinkDeviceResponse;
import com.energymonitor.device.adapter.in.web.dto.UpdateDeviceRequest;
import com.energymonitor.device.application.command.LinkDeviceCommand;
import com.energymonitor.device.application.command.UnlinkDeviceCommand;
import com.energymonitor.device.application.command.UpdateDeviceCommand;
import com.energymonitor.device.application.port.in.LinkDevice;
import com.energymonitor.device.application.port.in.ListHomeDevices;
import com.energymonitor.device.application.port.in.ReissueDeviceCredentials;
import com.energymonitor.device.application.port.in.UnlinkDevice;
import com.energymonitor.device.application.port.in.UpdateDevice;
import com.energymonitor.device.application.result.LinkedDeviceResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Devices of a home. The caller comes from the access token; membership and the OWNER role are
 * checked by the use case.
 *
 * <p>Linking returns the broker address configured in {@code device.provisioning.*}: the web
 * app writes it to the module over Bluetooth together with the credentials, so the address of
 * the server is never compiled into the firmware. Reissuing the credentials returns the same
 * shape, so a module already linked can be given a new Wi-Fi network.
 */
@RestController
@RequestMapping("/api/v1/homes/{homeId}/devices")
public class HomeDeviceController {

    private final ListHomeDevices listHomeDevices;
    private final LinkDevice linkDevice;
    private final UnlinkDevice unlinkDevice;
    private final UpdateDevice updateDevice;
    private final ReissueDeviceCredentials reissueCredentials;
    private final CurrentUserResolver currentUser;
    private final LinkDeviceResponse.Broker broker;

    public HomeDeviceController(ListHomeDevices listHomeDevices, LinkDevice linkDevice, UnlinkDevice unlinkDevice,
                                UpdateDevice updateDevice, ReissueDeviceCredentials reissueCredentials,
                                CurrentUserResolver currentUser,
                                @Value("${device.provisioning.mqtt-host:}") String mqttHost,
                                @Value("${device.provisioning.mqtt-port:8883}") int mqttPort) {
        this.listHomeDevices = listHomeDevices;
        this.linkDevice = linkDevice;
        this.unlinkDevice = unlinkDevice;
        this.updateDevice = updateDevice;
        this.reissueCredentials = reissueCredentials;
        this.currentUser = currentUser;
        this.broker = new LinkDeviceResponse.Broker(mqttHost, mqttPort);
    }

    @GetMapping
    public List<HomeDeviceResponse> list(@PathVariable String homeId) {
        return listHomeDevices.list(currentUser.resolveCurrentUserId(), homeId).stream()
                .map(HomeDeviceResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkDeviceResponse link(@PathVariable String homeId, @Valid @RequestBody LinkDeviceRequest request) {
        LinkedDeviceResult result = linkDevice.link(new LinkDeviceCommand(currentUser.resolveCurrentUserId(),
                homeId, request.deviceCode(), request.name().trim(), request.applianceTypeId(),
                request.location()));
        return new LinkDeviceResponse(HomeDeviceResponse.from(result.device()), result.apiKey(), broker);
    }

    /** Changes what the module measures, its name or its room (OWNER only). */
    @PutMapping("/{deviceId}")
    public HomeDeviceResponse update(@PathVariable String homeId, @PathVariable String deviceId,
                                     @Valid @RequestBody UpdateDeviceRequest request) {
        return HomeDeviceResponse.from(updateDevice.update(new UpdateDeviceCommand(
                currentUser.resolveCurrentUserId(), homeId, deviceId, request.name().trim(),
                request.applianceTypeId(), request.location())));
    }

    /**
     * New API key and the current broker address for a module already linked, to write them over
     * Bluetooth with a new Wi-Fi network. The previous key stops working (OWNER only).
     */
    @PostMapping("/{deviceId}/credentials")
    public LinkDeviceResponse reissueCredentials(@PathVariable String homeId, @PathVariable String deviceId) {
        LinkedDeviceResult result = reissueCredentials.reissue(currentUser.resolveCurrentUserId(), homeId, deviceId);
        return new LinkDeviceResponse(HomeDeviceResponse.from(result.device()), result.apiKey(), broker);
    }

    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable String homeId, @PathVariable String deviceId) {
        unlinkDevice.unlink(new UnlinkDeviceCommand(currentUser.resolveCurrentUserId(), homeId, deviceId));
    }
}
