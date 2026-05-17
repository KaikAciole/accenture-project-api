package br.com.accenture.order.infrastructure.messaging;

import br.com.accenture.order.infrastructure.persistence.OutboxEventRepository;
import br.com.accenture.order.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxRelay relay;

    private static OutboxEventJpaEntity pendingEvent(String eventType, String payload) {
        return OutboxEventJpaEntity.builder()
                .id(UUID.randomUUID())
                .aggregateType("Order")
                .aggregateId(UUID.randomUUID().toString())
                .eventType(eventType)
                .payload(payload)
                .createdAt(Instant.now())
                .processed(false)
                .build();
    }

    @Test
    @DisplayName("Deve publicar eventos pendentes no Rabbit e marcar como processados")
    void shouldPublishPendingEventsAndMarkAsProcessed() {
        OutboxEventJpaEntity e1 = pendingEvent("order.created", "{\"a\":1}");
        OutboxEventJpaEntity e2 = pendingEvent("order.paid", "{\"b\":2}");
        when(repository.findByProcessedFalse()).thenReturn(List.of(e1, e2));

        relay.processOutbox();

        verify(rabbitTemplate).send(eq("order.exchange"), eq("order.created"), any(Message.class));
        verify(rabbitTemplate).send(eq("order.exchange"), eq("order.paid"), any(Message.class));

        ArgumentCaptor<OutboxEventJpaEntity> savedCaptor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository, org.mockito.Mockito.times(2)).save(savedCaptor.capture());

        assertThat(savedCaptor.getAllValues()).allMatch(OutboxEventJpaEntity::isProcessed);
    }

    @Test
    @DisplayName("Nao deve fazer nada quando nao ha eventos pendentes")
    void shouldDoNothingWhenNoPendingEvents() {
        when(repository.findByProcessedFalse()).thenReturn(List.of());

        relay.processOutbox();

        verify(rabbitTemplate, never()).send(any(), any(), any(Message.class));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Quando o envio para o Rabbit falha, o evento nao deve ser marcado como processado e o loop deve continuar")
    void shouldNotMarkAsProcessedWhenRabbitFailsAndContinueLoop() {
        OutboxEventJpaEntity failing = pendingEvent("order.created", "{\"x\":1}");
        OutboxEventJpaEntity working = pendingEvent("order.paid", "{\"y\":2}");
        when(repository.findByProcessedFalse()).thenReturn(List.of(failing, working));

        doThrow(new AmqpException("rabbit indisponivel"))
                .when(rabbitTemplate).send(eq("order.exchange"), eq("order.created"), any(Message.class));

        relay.processOutbox();

        verify(rabbitTemplate).send(eq("order.exchange"), eq("order.paid"), any(Message.class));

        ArgumentCaptor<OutboxEventJpaEntity> savedCaptor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getEventType()).isEqualTo("order.paid");
        assertThat(savedCaptor.getValue().isProcessed()).isTrue();
        assertThat(failing.isProcessed()).isFalse();
    }
}
