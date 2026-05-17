package br.com.accenture.auth;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class AuthApplicationMainTest {

    @Test
    void main_shouldDelegateToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            String[] args = {"--spring.main.web-application-type=none"};

            AuthApplication.main(args);

            mocked.verify(() -> SpringApplication.run(AuthApplication.class, args));
        }
    }
}
