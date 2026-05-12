package br.com.accenture.assistant.application.service;

import br.com.accenture.assistant.domain.gateway.AssistantGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private final AssistantGateway gateway;

    public String ask(String question) {
        return gateway.ask(question);
    }
}
