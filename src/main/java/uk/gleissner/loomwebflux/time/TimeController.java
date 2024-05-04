package uk.gleissner.loomwebflux.time;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.controller.LoomWebFluxController;

@RestController
@RequiredArgsConstructor
public class TimeController extends LoomWebFluxController {

    public static final String API_PATH = "/epoch-millis";

    private final Environment environment;

    @Getter(lazy = true)
    private final WebClient webClient = WebClient.create("http://localhost:" + environment.getProperty("local.server.port"));

    @GetMapping({PLATFORM_TOMCAT + API_PATH, LOOM_TOMCAT + API_PATH, LOOM_NETTY + API_PATH})
    @ResponseBody
    public Long epochMillisLoomNetty(@RequestParam String approach,
                                     @RequestParam Long delayInMillis,
                                     @RequestParam(defaultValue = "0") Integer delayCallDepth) throws InterruptedException {
        log("epochMillisLoom");
        waitOrFetchEpochMillis(getWebClient(), approach, delayCallDepth, delayInMillis);
        return System.currentTimeMillis();
    }

    @GetMapping(WEBFLUX_NETTY + API_PATH)
    @ResponseBody
    public Mono<Long> epochMillisWebfluxNetty(@RequestParam String approach,
                                              @RequestParam Long delayInMillis,
                                              @RequestParam(defaultValue = "0") Integer delayCallDepth) {
        log("epochMillisWebflux");
        return waitOrFetchEpochMillisMono(getWebClient(), approach, delayCallDepth, delayInMillis)
                .map(ignore -> System.currentTimeMillis());
    }
}
