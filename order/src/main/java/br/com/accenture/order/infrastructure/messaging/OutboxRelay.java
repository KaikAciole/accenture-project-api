package br.com.accenture.order.infrastructure.messaging;

import br.com.accenture.order.infrastructure.persistence.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxRelay(OutboxEventRepository repository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.delay:5000}")
    @Transactional
    public void processOutbox() {
        var pendingEvents = repository.findByProcessedFalse();

        for (var event : pendingEvents) {
            try {
                JsonNode payloadNode = objectMapper.readTree(event.getPayload());

                rabbitTemplate.convertAndSend("order.exchange", event.getEventType(), payloadNode);

                event.setProcessed(true);
                repository.save(event);

                log.debug("Outbox event processed: {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", event.getId(), e);
            }
        }
    }
}