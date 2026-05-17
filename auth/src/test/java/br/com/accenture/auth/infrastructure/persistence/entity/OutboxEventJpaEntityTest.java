package br.com.accenture.auth.infrastructure.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventJpaEntityTest {

    @Test
    void create_shouldInitializeAllFieldsWithProcessedFalse() {
        OutboxEventJpaEntity entity = OutboxEventJpaEntity.create(
                "User", "id-1", "user.registered", "{\"payload\":\"x\"}"
        );

        assertThat(entity.getAggregateType()).isEqualTo("User");
        assertThat(entity.getAggregateId()).isEqualTo("id-1");
        assertThat(entity.getEventType()).isEqualTo("user.registered");
        assertThat(entity.getPayload()).isEqualTo("{\"payload\":\"x\"}");
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.isProcessed()).isFalse();
    }

    @Test
    void settersShouldUpdateMutableFields() {
        OutboxEventJpaEntity entity = OutboxEventJpaEntity.create("A", "b", "c", "d");
        UUID id = UUID.randomUUID();
        Instant when = Instant.now();

        entity.setId(id);
        entity.setAggregateType("AA");
        entity.setAggregateId("bb");
        entity.setEventType("cc");
        entity.setPayload("dd");
        entity.setCreatedAt(when);
        entity.setProcessed(true);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getAggregateType()).isEqualTo("AA");
        assertThat(entity.getAggregateId()).isEqualTo("bb");
        assertThat(entity.getEventType()).isEqualTo("cc");
        assertThat(entity.getPayload()).isEqualTo("dd");
        assertThat(entity.getCreatedAt()).isEqualTo(when);
        assertThat(entity.isProcessed()).isTrue();
    }
}
