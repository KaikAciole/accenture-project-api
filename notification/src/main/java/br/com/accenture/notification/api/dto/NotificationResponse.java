package br.com.accenture.notification.api.dto;

import br.com.accenture.notification.domain.enums.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String recipient,
        String subject,
        String body,
        NotificationStatus status,
        Instant createdAt,
        Instant sentAt
) {
}
