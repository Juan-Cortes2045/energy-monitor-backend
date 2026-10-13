package com.energymonitor.home.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for handing a home over to one of its members.
 *
 * @param newOwnerId identifier of the member who becomes the owner
 */
public record TransferHomeOwnershipRequest(
        @NotBlank @Size(max = 10) String newOwnerId
) {
}
