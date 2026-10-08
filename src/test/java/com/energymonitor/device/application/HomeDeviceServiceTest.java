package com.energymonitor.device.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.energymonitor.device.api.DeviceConnectivityLost;
import com.energymonitor.device.api.DeviceConnectivityRestored;
import com.energymonitor.device.api.DeviceLinked;
import com.energymonitor.device.application.command.LinkDeviceCommand;
import com.energymonitor.device.application.command.UnlinkDeviceCommand;
import com.energymonitor.device.application.exception.ApplianceTypeNotFoundException;
import com.energymonitor.device.application.exception.DeviceAlreadyLinkedException;
import com.energymonitor.device.application.exception.HomeNotFoundException;
import com.energymonitor.device.application.exception.NotHomeOwnerException;
import com.energymonitor.device.application.port.out.ApplianceTypePersistencePort;
import com.energymonitor.device.application.port.out.DeviceEventPort;
import com.energymonitor.device.application.port.out.DeviceHomePersistencePort;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.application.port.out.DeviceStatusLogPersistencePort;
import com.energymonitor.device.application.port.out.HomeLookupPort;
import com.energymonitor.device.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.device.application.usecase.HomeDeviceService;
import com.energymonitor.device.domain.model.Device;
import com.energymonitor.device.domain.model.DeviceHome;
import com.energymonitor.device.domain.model.DeviceStatusLog;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HomeDeviceServiceTest {

    private static final String OWNER = "usr0000001";
    private static final String GUEST = "usr0000002";
    private static final String HOME = "hom0000001";
    private static final String OTHER_HOME = "hom0000002";

    private final Map<String, Device> devices = new HashMap<>();
    private final Map<String, String> homeByDevice = new HashMap<>();
    private int sequence;
    private final List<DeviceLinked> linkedEvents = new java.util.ArrayList<>();

    private final HomeDeviceService service = new HomeDeviceService(
            new DevicePersistencePort() {
                @Override
                public void save(Device device) {
                    devices.put(device.idDevice(), device);
                }

                @Override
                public Optional<Device> findById(String deviceId) {
                    return Optional.ofNullable(devices.get(deviceId));
                }

                @Override
                public Optional<Device> findByDeviceCode(String deviceCode) {
                    return devices.values().stream().filter(d -> d.deviceCode().equals(deviceCode)).findFirst();
                }
            },
            new DeviceHomePersistencePort() {
                @Override
                public void save(DeviceHome deviceHome) {
                    homeByDevice.put(deviceHome.deviceId(), deviceHome.homeId());
                }

                @Override
                public Optional<DeviceHome> findByDeviceId(String deviceId) {
                    return Optional.ofNullable(homeByDevice.get(deviceId)).map(h -> new DeviceHome(deviceId, h));
                }

                @Override
                public List<DeviceHome> findByHomeId(String homeId) {
                    return homeByDevice.entrySet().stream().filter(e -> e.getValue().equals(homeId))
                            .map(e -> new DeviceHome(e.getKey(), e.getValue())).toList();
                }

                @Override
                public void unlink(String deviceId) {
                    homeByDevice.remove(deviceId);
                }
            },
            new DeviceStatusLogPersistencePort() {
                @Override
                public void save(DeviceStatusLog log) {
                }

                @Override
                public Optional<DeviceStatusLog> findLatestByDeviceId(String deviceId) {
                    return Optional.empty();
                }

                @Override
                public void touch(String idDeviceStatus, Integer signalStrength, Instant lastSeen) {
                }

                @Override
                public List<DeviceStatusLog> findOnlineNotSeenSince(Instant cutoff) {
                    return List.of();
                }
            },
            new ApplianceTypePersistencePort() {
                @Override
                public boolean exists(String applianceTypeId) {
                    return "appl000001".equals(applianceTypeId);
                }

                @Override
                public Optional<String> findIdByName(String name) {
                    return Optional.empty();
                }

                @Override
                public List<ApplianceTypeEntry> findAll() {
                    return List.of(new ApplianceTypeEntry("appl000001", "refrigerator"));
                }
            },
            new HomeLookupPort() {
                @Override
                public boolean homeExists(String homeId) {
                    return HOME.equals(homeId) || OTHER_HOME.equals(homeId);
                }

                @Override
                public boolean isMember(String userId, String homeId) {
                    return (OWNER.equals(userId) || GUEST.equals(userId)) && homeExists(homeId);
                }

                @Override
                public boolean isOwner(String userId, String homeId) {
                    return OWNER.equals(userId) && homeExists(homeId);
                }
            },
            new IdentifierGeneratorPort() {
                @Override
                public String nextDeviceId() {
                    return String.format("dev%07d", ++sequence);
                }

                @Override
                public String nextDeviceStatusLogId() {
                    return String.format("dss%07d", ++sequence);
                }

                @Override
                public String nextApiKey() {
                    return "key-" + (++sequence);
                }
            },
            new DeviceEventPort() {
                @Override
                public void publish(DeviceConnectivityLost event) {
                }

                @Override
                public void publish(DeviceConnectivityRestored event) {
                }

                @Override
                public void publish(DeviceLinked event) {
                    linkedEvents.add(event);
                }
            },
            Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC));

    private LinkDeviceCommand link(String user, String home, String code) {
        return new LinkDeviceCommand(user, home, code, "Nevera", "appl000001", "kitchen");
    }

    @Test
    void ownerLinksANewModuleAndGetsItsKey() {
        var result = service.link(link(OWNER, HOME, "EM204"));

        assertEquals("refrigerator", result.device().applianceType());
        assertEquals(HOME, homeByDevice.get(result.device().idDevice()));
        assertEquals(result.apiKey(), devices.get(result.device().idDevice()).apiKey());
        assertEquals(1, service.list(GUEST, HOME).size());
        assertEquals(List.of(new DeviceLinked(result.device().idDevice(), HOME, "Nevera", OWNER,
                Instant.parse("2026-10-08T00:00:00Z"))), linkedEvents);
    }

    @Test
    void linkingTheSameModuleAgainKeepsItsIdentityAndRotatesTheKey() {
        var first = service.link(link(OWNER, HOME, "EM204"));
        var second = service.link(link(OWNER, HOME, "EM204"));

        assertEquals(first.device().idDevice(), second.device().idDevice());
        assertNotEquals(first.apiKey(), second.apiKey());
        assertEquals(1, devices.size());
    }

    @Test
    void aModuleOfAnotherHomeCannotBeTaken() {
        service.link(link(OWNER, OTHER_HOME, "EM204"));

        assertThrows(DeviceAlreadyLinkedException.class, () -> service.link(link(OWNER, HOME, "EM204")));
    }

    @Test
    void onlyOwnersLinkAndOutsidersSeeNothing() {
        assertThrows(NotHomeOwnerException.class, () -> service.link(link(GUEST, HOME, "EM204")));
        assertThrows(HomeNotFoundException.class, () -> service.link(link("intruder", HOME, "EM204")));
        assertThrows(HomeNotFoundException.class, () -> service.list("intruder", HOME));
        assertThrows(ApplianceTypeNotFoundException.class, () -> service.link(
                new LinkDeviceCommand(OWNER, HOME, "EM204", "Nevera", "applXXXXXX", null)));
    }

    @Test
    void unlinkingDetachesTheModuleAndRevokesItsKey() {
        var linked = service.link(link(OWNER, HOME, "EM204"));
        String id = linked.device().idDevice();

        service.unlink(new UnlinkDeviceCommand(OWNER, HOME, id));

        assertTrue(service.list(OWNER, HOME).isEmpty());
        assertNotEquals(linked.apiKey(), devices.get(id).apiKey());
        // and it can be linked again, to any home
        service.link(link(OWNER, OTHER_HOME, "EM204"));
        assertEquals(OTHER_HOME, homeByDevice.get(id));
    }
}
