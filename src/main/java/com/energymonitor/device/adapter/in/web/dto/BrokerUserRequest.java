package com.energymonitor.device.adapter.in.web.dto;

/**
 * Body the broker auth plugin (mosquitto-go-auth, HTTP backend, JSON params) sends to
 * authenticate a client on connect.
 */
public record BrokerUserRequest(String username, String password, String clientid) {
}
