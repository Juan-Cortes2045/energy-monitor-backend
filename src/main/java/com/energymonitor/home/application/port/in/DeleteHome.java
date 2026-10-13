package com.energymonitor.home.application.port.in;

import com.energymonitor.home.application.command.DeleteHomeCommand;

/**
 * Use case: an owner deletes one of their homes (soft delete).
 */
public interface DeleteHome {

    void delete(DeleteHomeCommand command);
}
