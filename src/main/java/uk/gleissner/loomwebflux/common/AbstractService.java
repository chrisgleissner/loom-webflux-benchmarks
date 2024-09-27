package uk.gleissner.loomwebflux.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.common.proxy.nonreactive.NonReactiveServiceProxy;
import uk.gleissner.loomwebflux.common.proxy.reactive.ReactiveServiceProxy;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractService {

    private final ReactiveServiceProxy reactiveClient;
    private final NonReactiveServiceProxy nonReactiveClient;

    protected Long waitOrFetchEpochMillis(int delayCallDepth, long delayInMillis) throws InterruptedException {
        if (delayCallDepth == 0) {
            Thread.sleep(Duration.ofMillis(delayInMillis));
            return System.currentTimeMillis();
        } else {
            return nonReactiveClient.fetchEpochMillis(delayCallDepth - 1, delayInMillis);
        }
    }

    protected Mono<Long> waitOrFetchEpochMillisReactive(int delayCallDepth, long delayInMillis) {
        return Mono
            .delay(Duration.ofMillis(delayCallDepth == 0 ? delayInMillis : 0))
            .flatMap(d -> delayCallDepth > 0
                ? reactiveClient.fetchEpochMillis(delayCallDepth - 1, delayInMillis)
                : Mono.just(System.currentTimeMillis()));
    }

    protected void log(String methodName) {
        if (log.isDebugEnabled()) {
            log.debug("{}: thread={}", methodName, Thread.currentThread());
        }
    }
}
