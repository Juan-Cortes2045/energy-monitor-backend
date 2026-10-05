package com.energymonitor.alert.infrastructure;

import com.energymonitor.alert.api.AlertApi;
import com.energymonitor.alert.api.AlertDto;
import com.energymonitor.alert.api.AlertStatus;
import com.energymonitor.alert.application.port.out.AlertPersistencePort;
import com.energymonitor.alert.domain.model.Alert;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AlertApi} for consumption by other modules.
 */
@Component
public class AlertApiImpl implements AlertApi {

    private final AlertPersistencePort alertPort;

    public AlertApiImpl(AlertPersistencePort alertPort) {
        this.alertPort = alertPort;
    }

    @Override
    public Optional<AlertDto> findAlert(String idAlert) {
        return alertPort.findActive(idAlert)
                .map(AlertApiImpl::toDto);
    }

    @Override
    public List<AlertDto> listPendingByHome(String homeId) {
        return alertPort.listActiveByHome(homeId, AlertStatus.PENDING).stream()
                .map(AlertApiImpl::toDto)
                .toList();
    }

    private static AlertDto toDto(Alert alert) {
        return new AlertDto(alert.idAlert(), alert.homeId(), alert.deviceId(), alert.type(),
                alert.messageKey(), alert.dateTime(), alert.alertStatus(),
                alert.consumptionLevelId(), alert.measurementId());
    }
}
