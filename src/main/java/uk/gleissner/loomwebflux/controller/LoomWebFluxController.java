package uk.gleissner.loomwebflux.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.time.TimeController;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public abstract class LoomWebFluxController {

    private final Environment environment;

    @Getter(lazy = true)
    private final WebClient webClient = WebClient.create("http://localhost:" + environment.getProperty("local.server.port"));

    protected Mono<Long> fetchEpochMillis(String approach, Integer delayCallDepth, Long delayInMillis) {
        return getWebClient().get().uri(uriBuilder -> uriBuilder
                        .path("/" + approach + TimeController.API_PATH)
                        .queryParam("delayCallDepth", delayCallDepth)
                        .queryParam("delayInMillis", delayInMillis)
                        .build()).retrieve()
                .bodyToMono(Long.class);
    }

    protected Long waitOrFetchEpochMillis(String approach, int delayCallDepth, long delayInMillis) throws InterruptedException {
        if (delayCallDepth == 0) {
            Thread.sleep(Duration.ofMillis(delayInMillis));
            return System.currentTimeMillis();
        } else {
            return fetchEpochMillis(approach, delayCallDepth - 1, delayInMillis).block();
        }
    }

    protected Mono<Long> waitOrFetchEpochMillisReactive(String approach, int delayCallDepth, long delayInMillis) {
        return Mono
                .delay(Duration.ofMillis(delayCallDepth == 0 ? delayInMillis : 0))
                .flatMap(d -> delayCallDepth > 0
                        ? fetchEpochMillis(approach, delayCallDepth - 1, delayInMillis)
                        : Mono.just(System.currentTimeMillis()));
    }

    protected void log(String methodName) {
        if (log.isDebugEnabled()) {
            log.debug("{}: thread={}", methodName, Thread.currentThread());
        }
    }
}
