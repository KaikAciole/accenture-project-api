package br.com.accenture.auth.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String eventType; // Ex: "user.registered"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // O JSON do evento

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public OutboxEventJpaEntity(String eventType, String payload) {
        this.eventType = eventType;
        this.payload = payload;
    }
}