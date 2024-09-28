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
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import uk.gleissner.loomwebflux.config.AppProperties;

import java.util.function.Function;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;

@Configuration
@RequiredArgsConstructor
@Slf4j
class WebClientReactorNettyConfig {

    static final String REACTOR_NETTY_HTTP_CLIENT = "Reactor Netty HttpClient";

    private final Environment environment;
    private final AppProperties appProperties;

    static Function<HttpClient, HttpClient> reactorNettyHttpClientConfigurer(AppProperties appProperties) {
        return httpClient -> httpClient
            .responseTimeout(appProperties.client().responseTimeout())
            .option(CONNECT_TIMEOUT_MILLIS, (int) (appProperties.client().connectTimeout().toMillis()));
    }

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        val baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        log.info("Create WebClient based on " + REACTOR_NETTY_HTTP_CLIENT + " for {}", baseUrl);

        return builder
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(reactorResourceFactory(), reactorNettyHttpClientConfigurer(appProperties)))
            .build();
    }

    private ReactorResourceFactory reactorResourceFactory() {
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
