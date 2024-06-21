package uk.gleissner.loomwebflux.fixture;

import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.web.reactive.server.WebTestClient;

@Configuration
class WebTestConfig {


    @Bean
    @Lazy
    WebTestClient client(@LocalServerPort int port) {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
