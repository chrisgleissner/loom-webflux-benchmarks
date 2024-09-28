package uk.gleissner.loomwebflux.common.proxy.reactive;

import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.common.proxy.ServiceProxy;

public interface ReactiveServiceProxy extends ServiceProxy<Mono<Long>> {
}
