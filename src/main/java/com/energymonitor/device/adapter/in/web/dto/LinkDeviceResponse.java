package com.energymonitor.device.adapter.in.web.dto;

/**
 * What the browser writes to the module over Bluetooth once it is linked: its identity, its
 * secret and where the broker is. {@code apiKey} is only ever returned here.
 */
public record LinkDeviceResponse(HomeDeviceResponse device, String apiKey, Broker broker) {

    public record Broker(String host, int port) {
    }
}
