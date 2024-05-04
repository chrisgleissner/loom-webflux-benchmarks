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

    protected Mono<Long> fetchEpochMillisMono(String approach, Long delayInMillis, Integer delayCallDepth) {
        return getWebClient().get().uri(uriBuilder -> uriBuilder
                        .path("/" + approach + TimeController.API_PATH)
                        .queryParam("approach", approach)
                        .queryParam("delayCallDepth", delayCallDepth)
                        .queryParam("delayInMillis", delayInMillis)
                        .build()).retrieve()
                .bodyToMono(Long.class);
    }

    protected Long waitOrFetchEpochMillis(String approach, int delayCallDepth, long delayInMillis) throws InterruptedException {
        if (delayCallDepth == 0) {
            Thread.sleep(Duration.ofMillis(delayInMillis));
            return 0L;
        } else {
            return fetchEpochMillisMono(approach, delayInMillis, delayCallDepth - 1).block();
        }
    }

    protected Mono<Long> waitOrFetchEpochMillisMono(String approach, int delayCallDepth, long delayInMillis) {
        return Mono
                .delay(Duration.ofMillis(delayCallDepth == 0 ? delayInMillis : 0))
                .flatMap(d -> delayCallDepth > 0 ? fetchEpochMillisMono(approach, delayInMillis, delayCallDepth - 1) : Mono.just(0L));
    }

    protected void log(String methodName) {
        if (log.isDebugEnabled()) {
            log.debug("{}: thread={}", methodName, Thread.currentThread());
        }
    }
}
