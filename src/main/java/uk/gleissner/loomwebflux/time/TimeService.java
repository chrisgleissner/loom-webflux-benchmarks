package uk.gleissner.loomwebflux.time;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.common.AbstractService;
import uk.gleissner.loomwebflux.common.proxy.nonreactive.NonReactiveServiceProxy;
import uk.gleissner.loomwebflux.common.proxy.reactive.ReactiveServiceProxy;

@Service
public class TimeService extends AbstractService {

    TimeService(ReactiveServiceProxy reactiveClient, NonReactiveServiceProxy nonReactiveClient) {
        super(reactiveClient, nonReactiveClient);
    }

    public Long epochMillis(Long delayInMillis, Integer delayCallDepth) throws InterruptedException {
        log("epochMillis");
        return waitOrFetchEpochMillis(delayCallDepth, delayInMillis);
    }

    public Mono<Long> epochMillisReactive(Long delayInMillis, Integer delayCallDepth) {
        log("epochMillisReactive");
        return waitOrFetchEpochMillisReactive(delayCallDepth, delayInMillis);
    }

}
