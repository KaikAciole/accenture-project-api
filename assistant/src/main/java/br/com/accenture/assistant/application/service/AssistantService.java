package br.com.accenture.assistant.application.service;

import br.com.accenture.assistant.domain.gateway.AssistantGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private final AssistantGateway gateway;

    public Flux<String> ask(String question) {
        return gateway.ask(question);
    }
}
