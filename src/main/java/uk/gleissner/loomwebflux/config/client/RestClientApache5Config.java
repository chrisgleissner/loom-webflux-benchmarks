package uk.gleissner.loomwebflux.config.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import uk.gleissner.loomwebflux.config.AppProperties;

import static uk.gleissner.loomwebflux.config.Profiles.REST_CLIENT_APACHE5;

@Profile(REST_CLIENT_APACHE5)
@Configuration
@RequiredArgsConstructor
@Slf4j
class RestClientApache5Config extends AbstractRestClientConfig {

    private final Environment environment;
    private final AppProperties appProperties;

    @Bean
    RestClient restClient() {
        val requestFactory = new HttpComponentsClientHttpRequestFactory(HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(appProperties.client().connectTimeout()))
                .setResponseTimeout(Timeout.of(appProperties.client().responseTimeout()))
                .build())
            .setConnectionManager(connectionManager())
            .setKeepAliveStrategy((response, context) -> TimeValue.ofSeconds(5))
            .evictIdleConnections(TimeValue.ofSeconds(5))
            .evictExpiredConnections()
            .build());
        return restClient(environment, requestFactory, "Apache HttpClient 5");
    }

    private PoolingHttpClientConnectionManager connectionManager() {
        val connectionManager = new PoolingHttpClientConnectionManager();
        val maxConnections = appProperties.client().maxConnections();
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnections);
        return connectionManager;
    }
}