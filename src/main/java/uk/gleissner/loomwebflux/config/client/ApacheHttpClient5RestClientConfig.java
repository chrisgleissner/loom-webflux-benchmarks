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

@Profile("restclient-apache5")
@Configuration
@RequiredArgsConstructor
@Slf4j
class ApacheHttpClient5RestClientConfig {

    private final Environment environment;
    private final AppProperties appProperties;

    @Bean
    RestClient restClient() {
        val baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        log.info("Create RestClient based on Apache HttpClient 5 for {}", baseUrl);

        val requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.of(appProperties.client().connectTimeout()))
            .setResponseTimeout(Timeout.of(appProperties.client().responseTimeout()))
            .build();

        val maxTotalConnections = 30_000;
        val connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxTotalConnections);

        val httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .setKeepAliveStrategy((response, context) -> TimeValue.ofSeconds(5))
            .evictIdleConnections(TimeValue.ofSeconds(5))
            .evictExpiredConnections()
            .build();

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
            .build();
    }

}