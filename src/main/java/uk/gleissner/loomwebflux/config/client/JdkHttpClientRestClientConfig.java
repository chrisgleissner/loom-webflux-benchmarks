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

@Profile("restclient-jdk")
@Configuration
@RequiredArgsConstructor
@Slf4j
class JdkHttpClientRestClientConfig {

    private final Environment environment;
    private final AppProperties appProperties;

    @Bean
    RestClient restClient() {
        val baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        log.info("Create RestClient based on JDK HttpClient for {}", baseUrl);

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(appProperties.client().connectTimeout())
                .build()))
            .build();
    }
}
