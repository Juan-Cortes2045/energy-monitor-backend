package com.energymonitor.home.adapter.in.internal;

import com.energymonitor.home.application.port.in.CheckAccountDeletion;
import com.energymonitor.security.api.AccountDeletionBlockedException;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asked by the security service before it deletes an account: 204 lets it go, 409 refuses it
 * with the message the user sees. Only the security service reaches it; see
 * {@code InternalApiSecurityConfiguration}.
 */
@Hidden
@RestController
public class AccountDeletionCheckController {

    private final CheckAccountDeletion checkAccountDeletion;

    public AccountDeletionCheckController(CheckAccountDeletion checkAccountDeletion) {
        this.checkAccountDeletion = checkAccountDeletion;
    }

    @GetMapping("/internal/accounts/{userId}/deletion-check")
    public ResponseEntity<Map<String, String>> check(@PathVariable String userId) {
        try {
            checkAccountDeletion.ensureDeletable(userId);
            return ResponseEntity.noContent().build();
        } catch (AccountDeletionBlockedException blocked) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", blocked.getMessage()));
        }
    }
}
