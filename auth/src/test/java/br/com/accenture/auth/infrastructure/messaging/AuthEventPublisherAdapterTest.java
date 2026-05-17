package br.com.accenture.auth.infrastructure.messaging;

import br.com.accenture.auth.domain.enums.Role;
import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.infrastructure.persistence.entity.OutboxEventJpaEntity;
import br.com.accenture.auth.infrastructure.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthEventPublisherAdapterTest {

    @Mock
    private OutboxEventRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuthEventPublisherAdapter adapter;

    @Test
    void publishUserRegisteredEvent_shouldSaveOutboxEntity() throws JsonProcessingException {
        UUID customerId = UUID.randomUUID();
        UserCredential user = UserCredential.restore(
                UUID.randomUUID(), customerId, "user@example.com", "$2a$10$h",
                Set.of(Role.CUSTOMER), true, Instant.now(), Instant.now()
        );
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"json\":\"payload\"}");

        adapter.publishUserRegisteredEvent(user);

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEventJpaEntity saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("User");
        assertThat(saved.getAggregateId()).isEqualTo(customerId.toString());
        assertThat(saved.getEventType()).isEqualTo("user.registered");
        assertThat(saved.getPayload()).isEqualTo("{\"json\":\"payload\"}");
    }

    @Test
    void publishUserRegisteredEvent_shouldWrapSerializationFailure() throws JsonProcessingException {
        UserCredential user = UserCredential.restore(
                UUID.randomUUID(), UUID.randomUUID(), "user@example.com", "$2a$10$h",
                Set.of(Role.CUSTOMER), true, Instant.now(), Instant.now()
        );
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> adapter.publishUserRegisteredEvent(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao serializar");
    }

    @Test
    void publishPasswordResetRequestedEvent_shouldSaveOutboxEntity() throws JsonProcessingException {
        UUID customerId = UUID.randomUUID();
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"json\":\"reset\"}");

        adapter.publishPasswordResetRequestedEvent(customerId, "user@example.com", "plain-token");

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEventJpaEntity saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("User");
        assertThat(saved.getAggregateId()).isEqualTo(customerId.toString());
        assertThat(saved.getEventType()).isEqualTo("password.reset.requested");
        assertThat(saved.getPayload()).isEqualTo("{\"json\":\"reset\"}");
    }

    @Test
    void publishPasswordResetRequestedEvent_shouldWrapSerializationFailure() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> adapter.publishPasswordResetRequestedEvent(
                UUID.randomUUID(), "user@example.com", "plain-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("password reset");
    }
}
