package uk.gleissner.loomwebflux.time;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.controller.LoomWebFluxController;

import static uk.gleissner.loomwebflux.Approaches.LOOM_NETTY;
import static uk.gleissner.loomwebflux.Approaches.LOOM_TOMCAT;
import static uk.gleissner.loomwebflux.Approaches.PLATFORM_TOMCAT;
import static uk.gleissner.loomwebflux.Approaches.WEBFLUX_NETTY;

@RestController
public class TimeController extends LoomWebFluxController {

    public static final String API_PATH = "/epoch-millis";

    @Autowired
    TimeController(Environment environment) {
        super(environment);
    }

    @GetMapping(PLATFORM_TOMCAT + API_PATH)
    @ResponseBody
    public Long epochMillisPlatformTomcat(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) throws InterruptedException {
        return epochMillis(PLATFORM_TOMCAT, delayInMillis, delayCallDepth);
    }

    @GetMapping(LOOM_TOMCAT + API_PATH)
    @ResponseBody
    public Long epochMillisLoomTomcat(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) throws InterruptedException {
        return epochMillis(LOOM_TOMCAT, delayInMillis, delayCallDepth);
    }

    @GetMapping(LOOM_NETTY + API_PATH)
    @ResponseBody
    public Long epochMillisLoomNetty(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) throws InterruptedException {
        return epochMillis(LOOM_NETTY, delayInMillis, delayCallDepth);
    }

    private Long epochMillis(String approach, Long delayInMillis, Integer delayCallDepth) throws InterruptedException {
        log("epochMillisLoom");
        return waitOrFetchEpochMillis(approach, delayCallDepth, delayInMillis);
    }

    @GetMapping(WEBFLUX_NETTY + API_PATH)
    @ResponseBody
    public Mono<Long> epochMillisReactive(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) {
        log("epochMillisWebflux");
        return waitOrFetchEpochMillisReactive(WEBFLUX_NETTY, delayCallDepth, delayInMillis);
    }
}
