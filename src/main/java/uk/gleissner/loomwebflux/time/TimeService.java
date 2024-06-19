package uk.gleissner.loomwebflux.time;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.common.AbstractService;

@Service
public class TimeService extends AbstractService {

    TimeService(WebClient webClient) {
        super(webClient);
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
