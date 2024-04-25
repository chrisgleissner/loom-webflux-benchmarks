package uk.gleissner.loomwebflux.time;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.config.AppProperties;
import uk.gleissner.loomwebflux.controller.LoomWebFluxController;

@RestController
@RequiredArgsConstructor
public class TimeController extends LoomWebFluxController {

    private static final String API_PATH = "/epoch-millis";
    private final AppProperties appProperties;

    @GetMapping({PLATFORM_TOMCAT + API_PATH, LOOM_TOMCAT + API_PATH, LOOM_NETTY + API_PATH})
    @ResponseBody
    public Long epochMillisLoomNetty(@RequestParam(required = false) Long delayInMillis) throws InterruptedException {
        log("epochMillisLoom");
        Thread.sleep(actualDelay(delayInMillis, appProperties));
        return System.currentTimeMillis();
    }

    @GetMapping(WEBFLUX_NETTY + API_PATH)
    @ResponseBody
    public Mono<Long> epochMillisWebfluxNetty(@RequestParam(required = false) Long delayInMillis) {
        log("epochMillisWebflux");
        return Mono.delay(actualDelay(delayInMillis, appProperties))
                .map(d -> System.currentTimeMillis());
    }
}
