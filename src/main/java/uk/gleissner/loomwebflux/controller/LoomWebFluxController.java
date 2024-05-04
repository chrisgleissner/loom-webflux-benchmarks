package uk.gleissner.loomwebflux.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.time.TimeController;

import java.time.Duration;

@Slf4j
public abstract class LoomWebFluxController {
    public static final String PLATFORM_TOMCAT = "platform-tomcat";
    public static final String LOOM_TOMCAT = "loom-tomcat";
    public static final String LOOM_NETTY = "loom-netty";
    public static final String WEBFLUX_NETTY = "webflux-netty";

    protected String approach(HttpRequest request) {
        return request.getURI().getPath().split("/")[0];
    }

    protected Mono<Long> fetchEpochMillisMono(WebClient webClient, String approach, Long delayInMillis, Integer delayCallDepth) {
        return webClient.get().uri(uriBuilder -> uriBuilder
                        .path("/" + approach + TimeController.API_PATH)
                        .queryParam("approach", approach)
                        .queryParam("delayCallDepth", delayCallDepth)
                        .queryParam("delayInMillis", delayInMillis)
                        .build()).retrieve()
                .bodyToMono(Long.class);
    }

    protected Long waitOrFetchEpochMillis(WebClient webClient, String approach, int delayCallDepth, long delayInMillis) throws InterruptedException {
        if (delayCallDepth == 0) {
            Thread.sleep(Duration.ofMillis(delayInMillis));
            return 0L;
        } else {
            return fetchEpochMillisMono(webClient, approach, delayInMillis, delayCallDepth - 1).block();
        }
    }

    protected Mono<Long> waitOrFetchEpochMillisMono(WebClient webClient, String approach, int delayCallDepth, long delayInMillis) {
        return Mono
                .delay(Duration.ofMillis(delayCallDepth == 0 ? delayInMillis : 0))
                .flatMap(d -> delayCallDepth > 0 ? fetchEpochMillisMono(webClient, approach, delayInMillis, delayCallDepth - 1) : Mono.just(0L));
    }

    protected void log(String methodName) {
        if (log.isDebugEnabled()) {
            log.debug("{}: thread={}", methodName, Thread.currentThread());
        }
    }
}
