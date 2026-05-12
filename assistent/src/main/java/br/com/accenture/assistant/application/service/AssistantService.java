package br.com.accenture.assistent.application.service;

import br.com.accenture.assistent.domain.gateway.AssistantGateway;
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
