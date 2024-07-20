package uk.gleissner.loomwebflux.fixture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.web.reactive.server.WebTestClient;

@Configuration
class WebTestConfig {

    @Bean
    @Lazy
    WebTestClient client(@Value("${local.server.port}") int port) {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
