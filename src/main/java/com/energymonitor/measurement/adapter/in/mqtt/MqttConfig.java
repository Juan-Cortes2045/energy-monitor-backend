package com.energymonitor.measurement.adapter.in.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

/**
 * MQTT inbound wiring: one subscription and one channel per topic.
 *
 * <p>Each consumer listens on its own channel. A {@link DirectChannel} shared by two
 * subscribers would round-robin the messages between them, so every other telemetry message
 * would reach the status consumer.
 *
 * <p>The client ids get a {@code -telemetry} / {@code -status} suffix. The broker disconnects a
 * client when another one connects with the same id, so two backend instances need different
 * {@code mqtt.client-id} values.
 *
 * <p>{@link EnableIntegration} is explicit because the project depends on
 * {@code spring-integration-mqtt} without Spring Boot's integration starter; without it the
 * {@code @ServiceActivator} methods would never be subscribed to their channels.
 */
@Configuration
@EnableIntegration
public class MqttConfig {

    @Value("${mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client-id:energy-monitor-backend}")
    private String clientId;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    @Value("${mqtt.topics.telemetry:energy-monitor/devices/+/telemetry}")
    private String telemetryTopic;

    @Value("${mqtt.topics.status:energy-monitor/devices/+/status}")
    private String statusTopic;

    @Value("${mqtt.qos:0}")
    private int qos;

    @Value("${mqtt.keep-alive-interval:60}")
    private int keepAliveInterval;

    @Value("${mqtt.connection-timeout:30}")
    private int connectionTimeout;

    @Value("${mqtt.clean-session:true}")
    private boolean cleanSession;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        if (username != null && !username.isBlank()) {
            options.setUserName(username);
        }
        if (password != null && !password.isBlank()) {
            options.setPassword(password.toCharArray());
        }
        options.setKeepAliveInterval(keepAliveInterval);
        options.setConnectionTimeout(connectionTimeout);
        options.setCleanSession(cleanSession);
        options.setAutomaticReconnect(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttTelemetryChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttStatusChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inboundTelemetry(MqttPahoClientFactory factory) {
        return inbound(factory, "-telemetry", telemetryTopic, mqttTelemetryChannel());
    }

    @Bean
    public MessageProducer inboundStatus(MqttPahoClientFactory factory) {
        return inbound(factory, "-status", statusTopic, mqttStatusChannel());
    }

    private MessageProducer inbound(MqttPahoClientFactory factory, String suffix, String topic,
                                    MessageChannel channel) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + suffix, factory, topic);
        adapter.setQos(qos);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setOutputChannel(channel);
        return adapter;
    }
}
