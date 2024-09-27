package uk.gleissner.loomwebflux.common.proxy.nonreactive;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static uk.gleissner.loomwebflux.time.TimeController.NON_REACTIVE;

@Profile("restclient")
@Component
@RequiredArgsConstructor
public class NonReactiveRestClientServiceProxy implements NonReactiveServiceProxy {

    private final RestClient restClient;

    @Override
    public Long fetchEpochMillis(Integer delayCallDepth, Long delayInMillis) {
        return restClient
            .get()
            .uri(uriFunction(NON_REACTIVE, delayCallDepth, delayInMillis))
            .retrieve()
            .body(Long.class);
    }
}
