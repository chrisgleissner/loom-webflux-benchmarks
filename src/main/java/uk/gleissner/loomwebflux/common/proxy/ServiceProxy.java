package uk.gleissner.loomwebflux.common.proxy;

import org.springframework.web.util.UriBuilder;
import uk.gleissner.loomwebflux.time.TimeController;

import java.net.URI;
import java.util.function.Function;

public interface ServiceProxy<T> {

    T fetchEpochMillis(Integer delayCallDepth, Long delayInMillis);

    default Function<UriBuilder, URI> uriFunction(String approachGroup, Integer delayCallDepth, Long delayInMillis) {
        return uriBuilder -> uriBuilder
            .path("/" + approachGroup + TimeController.API_PATH)
            .queryParam("delayCallDepth", delayCallDepth)
            .queryParam("delayInMillis", delayInMillis)
            .build();
    }
}
