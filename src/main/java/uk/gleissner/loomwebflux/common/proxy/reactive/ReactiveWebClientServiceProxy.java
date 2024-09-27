package uk.gleissner.loomwebflux.common.proxy.reactive;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static uk.gleissner.loomwebflux.time.TimeController.REACTIVE;

@Component
@RequiredArgsConstructor
public class ReactiveWebClientServiceProxy implements ReactiveServiceProxy {

    private final WebClient webClient;

    @Override
    public Mono<Long> fetchEpochMillis(Integer delayCallDepth, Long delayInMillis) {
        return fetchEpochMillis(REACTIVE, delayCallDepth, delayInMillis);
    }

    public Mono<Long> fetchEpochMillis(String approachGroup, Integer delayCallDepth, Long delayInMillis) {
        return webClient
            .get()
            .uri(uriFunction(approachGroup, delayCallDepth, delayInMillis))
            .retrieve()
            .bodyToMono(Long.class);
    }
}
