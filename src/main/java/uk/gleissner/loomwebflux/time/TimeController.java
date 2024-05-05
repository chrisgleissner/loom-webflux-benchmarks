package uk.gleissner.loomwebflux.time;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.controller.LoomWebFluxController;

import static uk.gleissner.loomwebflux.Approaches.LOOM_NETTY;
import static uk.gleissner.loomwebflux.Approaches.LOOM_TOMCAT;
import static uk.gleissner.loomwebflux.Approaches.PLATFORM_TOMCAT;
import static uk.gleissner.loomwebflux.Approaches.WEBFLUX_NETTY;

@RestController
public class TimeController extends LoomWebFluxController {

    public static final String API_PATH = "/epoch-millis";

    TimeController(WebClient webClient) {
        super(webClient);
    }

    @GetMapping({NON_REACTIVE + API_PATH, PLATFORM_TOMCAT + API_PATH, LOOM_TOMCAT + API_PATH, LOOM_NETTY + API_PATH})
    @ResponseBody
    public Long epochMillis(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) throws InterruptedException {
        log("epochMillis");
        return waitOrFetchEpochMillis(delayCallDepth, delayInMillis);
    }

    @GetMapping({REACTIVE + API_PATH, WEBFLUX_NETTY + API_PATH})
    @ResponseBody
    public Mono<Long> epochMillisReactive(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) {
        log("epochMillisReactive");
        return waitOrFetchEpochMillisReactive(delayCallDepth, delayInMillis);
    }
}
