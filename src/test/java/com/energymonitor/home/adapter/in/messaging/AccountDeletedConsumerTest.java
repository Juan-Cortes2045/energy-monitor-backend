package com.energymonitor.home.adapter.in.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.energymonitor.home.application.port.in.RemoveDeletedAccount;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Account deleted consumer")
class AccountDeletedConsumerTest {

    private static final String BODY =
            "{\"userId\":\"use0000009\",\"deletedAt\":\"2026-10-20T15:00:00Z\"}";

    private final RemoveDeletedAccount removeDeletedAccount = mock(RemoveDeletedAccount.class);
    private final AccountDeletedConsumer consumer = new AccountDeletedConsumer(removeDeletedAccount,
            mock(PlatformTransactionManager.class), JsonMapper.builder().build());

    @Test
    @DisplayName("removes the memberships of the deleted account")
    void removesMemberships() {
        consumer.on(message(BODY));

        verify(removeDeletedAccount).remove("use0000009");
    }

    private static Message message(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setType("account.deleted");
        properties.setMessageId("msg0000001");
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
