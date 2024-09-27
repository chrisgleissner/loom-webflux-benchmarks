package uk.gleissner.loomwebflux.config.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.resources.ConnectionProvider;
import uk.gleissner.loomwebflux.config.AppProperties;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;

@Configuration
@RequiredArgsConstructor
@Slf4j
class ReactorNettyWebClientConfig {

    private final Environment environment;
    private final AppProperties appProperties;

    @Bean
    WebClient webClient(WebClient.Builder builder, ReactorResourceFactory reactorResourceFactory) {
        val baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        log.info("Create WebClient based on Reactor Netty HttpClient for {}", baseUrl);

        val clientProps = appProperties.client();
        return builder
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(reactorResourceFactory,
                httpClient -> httpClient
                    .responseTimeout(clientProps.responseTimeout())
                    .option(CONNECT_TIMEOUT_MILLIS, (int) (clientProps.connectTimeout().toMillis()))
            ))
            .build();
    }

    @Bean
    public ReactorResourceFactory reactorResourceFactory() {
        val factory = new ReactorResourceFactory();
        factory.setUseGlobalResources(false);

        val clientProps = appProperties.client();
        factory.setConnectionProvider(ConnectionProvider.builder("custom")
            .maxConnections(clientProps.maxConnections())
            .pendingAcquireMaxCount(clientProps.pendingAcquireMaxCount())
            .pendingAcquireTimeout(clientProps.pendingAcquireTimeout())
            .build());
        return factory;
    }
}
