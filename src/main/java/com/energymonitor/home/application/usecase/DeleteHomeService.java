package com.energymonitor.home.application.usecase;

import com.energymonitor.home.api.HomeDeleted;
import com.energymonitor.home.application.command.DeleteHomeCommand;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.port.in.DeleteHome;
import com.energymonitor.home.application.port.out.HomeEventPort;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.application.port.out.HomeThresholdsPersistencePort;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.UserHome;
import java.time.Clock;

/**
 * Deletes a home (soft delete), at the request of one of its owners.
 *
 * <p><strong>Who.</strong> Any OWNER of the home, and only an owner: deleting the home takes it
 * away from every member, which is a decision of the people responsible for it, not a permission
 * a member could be granted. A member gets 403; somebody who is not in the home gets 404, so the
 * endpoint cannot be used to learn which homes exist.
 *
 * <p><strong>What goes with it.</strong>
 * <ul>
 *   <li>Every membership, and with it every favourite mark (favourite is a flag of the
 *       membership): the home disappears from every member's list.</li>
 *   <li>Its thresholds: they only make sense for the home, and are never reused.</li>
 *   <li>Its devices are not deleted but unlinked, by the device service reacting to
 *       {@code home.deleted}, which {@link HomeDeleted} is relayed as: a physical module outlives
 *       the home and can be linked to another one. Unlinking revokes its key and tells the module
 *       to offer Bluetooth again.</li>
 *   <li>Measurements, alerts and recommendations are kept: they belong to the device's history,
 *       and nobody can reach them through the deleted home any more because every membership is
 *       gone.</li>
 * </ul>
 *
 * <p>Everything is a soft delete ({@code deleted_at}), so the rows stay for auditing. The
 * writes and the event run in one transaction (see {@code TransactionalDeleteHome}). This
 * service contains no Spring annotations.
 */
public class DeleteHomeService implements DeleteHome {

    private final HomePersistencePort homePort;
    private final UserHomePersistencePort userHomePort;
    private final HomeThresholdsPersistencePort thresholdsPort;
    private final HomeEventPort events;
    private final Clock clock;

    public DeleteHomeService(HomePersistencePort homePort, UserHomePersistencePort userHomePort,
                             HomeThresholdsPersistencePort thresholdsPort, HomeEventPort events,
                             Clock clock) {
        this.homePort = homePort;
        this.userHomePort = userHomePort;
        this.thresholdsPort = thresholdsPort;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void delete(DeleteHomeCommand command) {
        UserHome caller = userHomePort.findActive(command.userId(), command.homeId())
                .orElseThrow(() -> new HomeNotFoundException(
                        "no active membership for user " + command.userId() + " in home " + command.homeId()));
        if (!caller.isOwner()) {
            throw new HomeAccessDeniedException(
                    "only an owner can delete home " + command.homeId());
        }
        HomeRemoval.remove(command.homeId(), command.userId(), homePort, userHomePort, thresholdsPort,
                events, clock);
    }
}
