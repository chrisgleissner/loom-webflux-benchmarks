package uk.gleissner.loomwebflux.config.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import uk.gleissner.loomwebflux.config.AppProperties;

import java.net.http.HttpClient;

import static java.net.http.HttpClient.Version.HTTP_1_1;
import static uk.gleissner.loomwebflux.config.Profiles.REST_CLIENT_JDK;

@Profile(REST_CLIENT_JDK)
@Configuration
@RequiredArgsConstructor
@Slf4j
class RestClientJdkConfig extends AbstractRestClientConfig {

    private final Environment environment;
    private final AppProperties appProperties;

    @Bean
    RestClient restClient() {
        val requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(appProperties.client().connectTimeout())
            .version(HTTP_1_1) // due to errors with HTTP_2
            .build());
        requestFactory.setReadTimeout(appProperties.client().responseTimeout());
        return restClient(environment, requestFactory, "JDK HttpClient");
    }
}
