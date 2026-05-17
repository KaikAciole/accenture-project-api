package br.com.accenture.auth.infrastructure.messaging;

import br.com.accenture.auth.infrastructure.persistence.entity.OutboxEventJpaEntity;
import br.com.accenture.auth.infrastructure.persistence.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayProcessorTest {

    @Mock
    private OutboxEventRepository outboxRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxRelayProcessor processor;

    @Test
    void processOutbox_shouldPublishAndMarkProcessedForEachEvent() {
        OutboxEventJpaEntity event1 = OutboxEventJpaEntity.create("User", "id1", "user.registered", "{\"a\":1}");
        OutboxEventJpaEntity event2 = OutboxEventJpaEntity.create("User", "id2", "password.reset.requested", "{\"b\":2}");
        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event1, event2));

        processor.processOutbox();

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("auth.exchange"), eq("user.registered"), messageCaptor.capture());
        verify(rabbitTemplate).send(eq("auth.exchange"), eq("password.reset.requested"), any(Message.class));
        verify(outboxRepository, times(2)).save(any(OutboxEventJpaEntity.class));
        assertThat(event1.isProcessed()).isTrue();
        assertThat(event2.isProcessed()).isTrue();
    }

    @Test
    void processOutbox_shouldStopAndNotMarkProcessedOnFailure() {
        OutboxEventJpaEntity event1 = OutboxEventJpaEntity.create("User", "id1", "user.registered", "{\"a\":1}");
        OutboxEventJpaEntity event2 = OutboxEventJpaEntity.create("User", "id2", "user.registered", "{\"b\":2}");
        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event1, event2));
        doThrow(new AmqpException("connection refused"))
                .when(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class));

        processor.processOutbox();

        verify(rabbitTemplate, times(1)).send(any(String.class), any(String.class), any(Message.class));
        verify(outboxRepository, never()).save(any());
        assertThat(event1.isProcessed()).isFalse();
        assertThat(event2.isProcessed()).isFalse();
    }

    @Test
    void processOutbox_shouldDoNothingWhenNoPendingEvents() {
        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of());

        processor.processOutbox();

        verify(rabbitTemplate, never()).send(any(String.class), any(String.class), any(Message.class));
        verify(outboxRepository, never()).save(any());
    }
}
