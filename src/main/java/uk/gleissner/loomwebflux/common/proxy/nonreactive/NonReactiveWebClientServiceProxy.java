package uk.gleissner.loomwebflux.common.proxy.nonreactive;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gleissner.loomwebflux.common.proxy.reactive.ReactiveWebClientServiceProxy;

import static uk.gleissner.loomwebflux.config.Profiles.NO_REST_CLIENT_PROFILE_ACTIVE;
import static uk.gleissner.loomwebflux.time.TimeController.NON_REACTIVE;

@Profile(NO_REST_CLIENT_PROFILE_ACTIVE)
@Component
@RequiredArgsConstructor
public class NonReactiveWebClientServiceProxy implements NonReactiveServiceProxy {

    private final ReactiveWebClientServiceProxy reactiveWebClientServiceProxy;

    @Override
    public Long fetchEpochMillis(Integer delayCallDepth, Long delayInMillis) {
        return reactiveWebClientServiceProxy
            .fetchEpochMillis(NON_REACTIVE, delayCallDepth, delayInMillis)
            .block();
    }
}
