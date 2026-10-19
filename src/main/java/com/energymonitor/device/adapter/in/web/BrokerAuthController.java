package com.energymonitor.device.adapter.in.web;

import com.energymonitor.device.adapter.in.web.dto.BrokerAclRequest;
import com.energymonitor.device.adapter.in.web.dto.BrokerUserRequest;
import com.energymonitor.device.application.port.in.AuthorizeBrokerAccess;
import com.energymonitor.device.application.port.in.AuthorizeBrokerAccess.Access;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints the MQTT broker calls to authenticate clients and check topic access.
 *
 * <p>The broker plugin runs in {@code response_mode status}: 200 grants, anything else denies.
 * These paths are not meant for browsers or devices; {@code BrokerAuthSecurityConfiguration}
 * only lets the networks in {@code mqtt.broker-auth.allowed-networks} reach them, and a public
 * reverse proxy must not forward {@code /internal/**}.
 */
@RestController
@RequestMapping("/internal/mqtt/auth")
public class BrokerAuthController {

    private static final Logger log = LoggerFactory.getLogger(BrokerAuthController.class);

    private final AuthorizeBrokerAccess authorizeBrokerAccess;

    public BrokerAuthController(AuthorizeBrokerAccess authorizeBrokerAccess) {
        this.authorizeBrokerAccess = authorizeBrokerAccess;
    }

    @PostMapping("/user")
    public ResponseEntity<Void> authenticate(@RequestBody BrokerUserRequest request) {
        boolean granted = authorizeBrokerAccess.authenticate(request.username(), request.password());
        if (!granted) {
            log.info("MQTT login refused for username '{}' (client '{}')", request.username(), request.clientid());
        }
        return decision(granted);
    }

    @PostMapping("/acl")
    public ResponseEntity<Void> authorize(@RequestBody BrokerAclRequest request) {
        boolean granted = switch (request.acc() == null ? 0 : request.acc()) {
            case 1 -> authorizeBrokerAccess.authorize(request.username(), request.topic(), Access.READ);
            case 2 -> authorizeBrokerAccess.authorize(request.username(), request.topic(), Access.WRITE);
            case 3 -> authorizeBrokerAccess.authorize(request.username(), request.topic(), Access.READ)
                    && authorizeBrokerAccess.authorize(request.username(), request.topic(), Access.WRITE);
            case 4 -> authorizeBrokerAccess.authorize(request.username(), request.topic(), Access.SUBSCRIBE);
            default -> false;
        };
        if (!granted) {
            log.info("MQTT access {} to '{}' refused for username '{}'", request.acc(), request.topic(),
                    request.username());
        }
        return decision(granted);
    }

    private static ResponseEntity<Void> decision(boolean granted) {
        return granted ? ResponseEntity.ok().build() : ResponseEntity.status(403).build();
    }
}
