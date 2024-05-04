package uk.gleissner.loomwebflux.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
class WebClientConfig {
    private final Environment environment;

    @Bean
    WebClient webClient() {
        return WebClient.create("http://localhost:" + environment.getProperty("local.server.port"));
    }
}
