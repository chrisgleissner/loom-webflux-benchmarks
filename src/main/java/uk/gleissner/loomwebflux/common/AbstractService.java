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
        // Mirror the non-reactive path's structure: only the leaf (depth 0) introduces the delay, while
        // intermediate levels recurse straight into the next call. The previous implementation issued a
        // Mono.delay(0) at every non-leaf level, adding a parallel-scheduler hop with no counterpart in
        // the blocking path and skewing the deep-call-stack comparison against the reactive approach.
        if (delayCallDepth > 0) {
            return reactiveClient.fetchEpochMillis(delayCallDepth - 1, delayInMillis);
        }
        return Mono.delay(Duration.ofMillis(delayInMillis)).map(ignored -> System.currentTimeMillis());
    }

    protected void log(String methodName) {
        if (log.isDebugEnabled()) {
            log.debug("{}: thread={}", methodName, Thread.currentThread());
        }
    }
}
