package br.com.accenture.auth.infrastructure.messaging;

import br.com.accenture.auth.infrastructure.persistence.entity.OutboxEventJpaEntity;
import br.com.accenture.auth.infrastructure.persistence.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayProcessor {

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE = "auth.exchange";

    @Scheduled(fixedDelayString = "${api.outbox.delay:5000}")
    @Transactional
    public void processOutbox() {
        List<OutboxEventJpaEntity> events = outboxRepository.findAll();

        for (OutboxEventJpaEntity event : events) {
            try {
                Message message = MessageBuilder
                        .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setContentEncoding(StandardCharsets.UTF_8.name())
                        .build();
                rabbitTemplate.send(EXCHANGE, event.getEventType(), message);
                outboxRepository.delete(event);
            } catch (Exception e) {
                log.error("Falha ao publicar evento da outbox. ID: {}", event.getId(), e);
                break;
            }
        }
    }
}