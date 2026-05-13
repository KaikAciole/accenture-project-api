package br.com.accenture.assistant.domain.gateway;

import reactor.core.publisher.Flux;

public interface AssistantGateway {

    Flux<String> ask(String question);
}
