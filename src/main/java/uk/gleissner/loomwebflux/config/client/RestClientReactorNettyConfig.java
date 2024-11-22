package uk.gleissner.loomwebflux.config.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.web.client.RestClient;
import uk.gleissner.loomwebflux.config.AppProperties;

import static uk.gleissner.loomwebflux.config.Profiles.REST_CLIENT_REACTOR_NETTY;
import static uk.gleissner.loomwebflux.config.client.WebClientReactorNettyConfig.REACTOR_NETTY_HTTP_CLIENT;
import static uk.gleissner.loomwebflux.config.client.WebClientReactorNettyConfig.reactorNettyHttpClientConfigurer;

@Profile(REST_CLIENT_REACTOR_NETTY)
@Configuration
@RequiredArgsConstructor
@Slf4j
class RestClientReactorNettyConfig extends AbstractRestClientConfig {

    private final Environment environment;
    private final AppProperties appProperties;

    @Bean
    RestClient restClient(ReactorResourceFactory reactorResourceFactory) {
        val requestFactory = new ReactorClientHttpRequestFactory(reactorResourceFactory, reactorNettyHttpClientConfigurer(appProperties));
        return restClient(environment, requestFactory, REACTOR_NETTY_HTTP_CLIENT);
    }
}