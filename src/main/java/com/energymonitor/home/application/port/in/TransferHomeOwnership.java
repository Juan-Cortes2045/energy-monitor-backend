package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.TransferHomeOwnershipCommand;

/**
 * Use case: the owner of a home hands it over to one of its members.
 */
public interface TransferHomeOwnership {

    void transfer(TransferHomeOwnershipCommand command);
}
