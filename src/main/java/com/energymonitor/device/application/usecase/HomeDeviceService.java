package com.energymonitor.device.application.usecase;

import com.energymonitor.device.application.command.LinkDeviceCommand;
import com.energymonitor.device.application.command.UnlinkDeviceCommand;
import com.energymonitor.device.application.exception.ApplianceTypeNotFoundException;
import com.energymonitor.device.application.exception.DeviceAlreadyLinkedException;
import com.energymonitor.device.application.exception.DeviceNotFoundException;
import com.energymonitor.device.application.exception.HomeNotFoundException;
import com.energymonitor.device.application.exception.NotHomeOwnerException;
import com.energymonitor.device.application.port.in.LinkDevice;
import com.energymonitor.device.application.port.in.ListApplianceTypes;
import com.energymonitor.device.application.port.in.ListHomeDevices;
import com.energymonitor.device.application.port.in.UnlinkDevice;
import com.energymonitor.device.application.port.out.ApplianceTypePersistencePort;
import com.energymonitor.device.application.port.out.ApplianceTypePersistencePort.ApplianceTypeEntry;
import com.energymonitor.device.api.DeviceLinked;
import com.energymonitor.device.application.port.out.DeviceEventPort;
import com.energymonitor.device.application.port.out.DeviceHomePersistencePort;
import com.energymonitor.device.application.port.out.DevicePersistencePort;
import com.energymonitor.device.application.port.out.DeviceStatusLogPersistencePort;
import com.energymonitor.device.application.port.out.HomeLookupPort;
import com.energymonitor.device.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.device.application.result.HomeDeviceResult;
import com.energymonitor.device.application.result.LinkedDeviceResult;
import com.energymonitor.device.domain.model.Device;
import com.energymonitor.device.domain.model.DeviceHome;
import com.energymonitor.device.domain.model.DeviceStatusLog;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devices as seen from a home: listing, linking and unlinking.
 *
 * <p>Access rules: any member may list; only an OWNER may link or unlink. A caller who is not a
 * member gets "home not found", so the endpoints cannot be used to probe home identifiers.
 */
@Service
@Transactional
public class HomeDeviceService implements LinkDevice, UnlinkDevice, ListHomeDevices, ListApplianceTypes {

    private final DevicePersistencePort devices;
    private final DeviceHomePersistencePort deviceHomes;
    private final DeviceStatusLogPersistencePort statusLogs;
    private final ApplianceTypePersistencePort applianceTypes;
    private final HomeLookupPort homes;
    private final IdentifierGeneratorPort identifiers;
    private final DeviceEventPort events;
    private final Clock clock;

    public HomeDeviceService(DevicePersistencePort devices, DeviceHomePersistencePort deviceHomes,
                             DeviceStatusLogPersistencePort statusLogs, ApplianceTypePersistencePort applianceTypes,
                             HomeLookupPort homes, IdentifierGeneratorPort identifiers, DeviceEventPort events,
                             Clock clock) {
        this.devices = devices;
        this.deviceHomes = deviceHomes;
        this.statusLogs = statusLogs;
        this.applianceTypes = applianceTypes;
        this.homes = homes;
        this.identifiers = identifiers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public LinkedDeviceResult link(LinkDeviceCommand command) {
        requireOwner(command.userId(), command.homeId());
        if (!applianceTypes.exists(command.applianceTypeId())) {
            throw new ApplianceTypeNotFoundException(command.applianceTypeId());
        }
        String apiKey = identifiers.nextApiKey();
        Instant now = clock.instant();

        Optional<Device> known = devices.findByDeviceCode(command.deviceCode());
        Device device;
        if (known.isPresent()) {
            Optional<DeviceHome> link = deviceHomes.findByDeviceId(known.get().idDevice());
            if (link.isPresent() && !link.get().homeId().equals(command.homeId())) {
                throw new DeviceAlreadyLinkedException(command.deviceCode());
            }
            device = known.get().relinked(command.name(), command.applianceTypeId(), command.location(), now, apiKey);
        } else {
            device = Device.create(identifiers.nextDeviceId(), command.name(), command.applianceTypeId(),
                    command.location(), null, now, command.deviceCode(), apiKey);
        }
        devices.save(device);
        deviceHomes.save(new DeviceHome(device.idDevice(), command.homeId()));
        events.publish(new DeviceLinked(device.idDevice(), command.homeId(), device.name(), command.userId(), now));
        return new LinkedDeviceResult(toResult(device, applianceTypeNames()), apiKey);
    }

    @Override
    public void unlink(UnlinkDeviceCommand command) {
        requireOwner(command.userId(), command.homeId());
        Device device = devices.findById(command.deviceId())
                .filter(d -> deviceHomes.findByDeviceId(d.idDevice())
                        .map(link -> link.homeId().equals(command.homeId()))
                        .orElse(false))
                .orElseThrow(() -> new DeviceNotFoundException(command.deviceId()));
        deviceHomes.unlink(device.idDevice());
        // The old key stops working; the module needs to be linked again to get a new one.
        devices.save(device.relinked(device.name(), device.applianceTypeId(), device.location(),
                device.installationDate(), identifiers.nextApiKey()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeDeviceResult> list(String userId, String homeId) {
        if (!homes.homeExists(homeId) || !homes.isMember(userId, homeId)) {
            throw new HomeNotFoundException(homeId);
        }
        Map<String, String> names = applianceTypeNames();
        return deviceHomes.findByHomeId(homeId).stream()
                .map(link -> devices.findById(link.deviceId()))
                .flatMap(Optional::stream)
                .map(device -> toResult(device, names))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplianceTypeEntry> list() {
        return applianceTypes.findAll();
    }

    private void requireOwner(String userId, String homeId) {
        if (!homes.homeExists(homeId) || !homes.isMember(userId, homeId)) {
            throw new HomeNotFoundException(homeId);
        }
        if (!homes.isOwner(userId, homeId)) {
            throw new NotHomeOwnerException(homeId);
        }
    }

    private Map<String, String> applianceTypeNames() {
        return applianceTypes.findAll().stream()
                .collect(Collectors.toMap(ApplianceTypeEntry::idApplianceType, ApplianceTypeEntry::name));
    }

    private HomeDeviceResult toResult(Device device, Map<String, String> applianceTypeNames) {
        Optional<DeviceStatusLog> current = statusLogs.findLatestByDeviceId(device.idDevice());
        return new HomeDeviceResult(device.idDevice(), device.name(), device.applianceTypeId(),
                applianceTypeNames.get(device.applianceTypeId()), device.location(), device.installationDate(),
                device.deviceCode(),
                current.map(log -> log.status().name()).orElse(null),
                current.map(DeviceStatusLog::signalStrength).orElse(null),
                current.map(DeviceStatusLog::lastSeen).orElse(null));
    }
}
