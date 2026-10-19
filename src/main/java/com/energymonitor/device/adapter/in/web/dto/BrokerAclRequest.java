package com.energymonitor.device.adapter.in.web.dto;

/**
 * Body the broker auth plugin sends to check a topic access.
 *
 * <p>{@code acc} follows Mosquitto: 1 read, 2 write, 3 read and write, 4 subscribe.
 */
public record BrokerAclRequest(String username, String clientid, String topic, Integer acc) {
}
