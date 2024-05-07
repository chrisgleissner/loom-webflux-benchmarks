package uk.gleissner.loomwebflux.config;

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

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;

@Configuration
@RequiredArgsConstructor
@Slf4j
class WebClientConfig {
    private final Environment environment;
    private final AppProperties appProperties;

    @Bean
    WebClient webClient(WebClient.Builder builder, ReactorResourceFactory reactorResourceFactory) {
        val baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        log.info("Create WebClient for {}", baseUrl);
        val props = appProperties.webClient();
        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(reactorResourceFactory,
                        httpClient -> httpClient
                                .responseTimeout(props.responseTimeout())
                                .option(CONNECT_TIMEOUT_MILLIS, (int) (props.connectTimeout().toMillis()))
                ))
                .build();
    }

    @Bean
    public ReactorResourceFactory reactorResourceFactory() {
        val factory = new ReactorResourceFactory();
        factory.setUseGlobalResources(false);
        val props = appProperties.webClient();
        factory.setConnectionProvider(ConnectionProvider.builder("custom")
                .maxConnections(props.maxConnections())
                .pendingAcquireMaxCount(props.pendingAcquireMaxCount())
                .pendingAcquireTimeout(props.pendingAcquireTimeout())
                .build());
        return factory;
    }
}
