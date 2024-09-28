package uk.gleissner.loomwebflux.config.client;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Slf4j
public abstract class AbstractRestClientConfig {

    protected RestClient restClient(Environment environment, ClientHttpRequestFactory clientHttpRequestFactory, String printableClientImplName) {
        val baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        log.info("Create RestClient based on {} for {}", printableClientImplName, baseUrl);
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(clientHttpRequestFactory)
            .build();
    }
}
