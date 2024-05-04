package uk.gleissner.loomwebflux.time;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.Approaches;
import uk.gleissner.loomwebflux.controller.LoomWebFluxController;

@RestController
public class TimeController extends LoomWebFluxController {

    public static final String API_PATH = "/epoch-millis";

    @Autowired
    TimeController(Environment environment) {
        super(environment);
    }

    @GetMapping(Approaches.PLATFORM_TOMCAT + API_PATH)
    @ResponseBody
    public Long epochMillisPlatformTomcat(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) throws InterruptedException {
        return epochMillis(Approaches.PLATFORM_TOMCAT, delayInMillis, delayCallDepth);
    }

    @GetMapping(Approaches.LOOM_TOMCAT + API_PATH)
    @ResponseBody
    public Long epochMillisLoomTomcat(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) throws InterruptedException {
        return epochMillis(Approaches.LOOM_TOMCAT, delayInMillis, delayCallDepth);
    }

    @GetMapping(Approaches.LOOM_NETTY + API_PATH)
    @ResponseBody
    public Long epochMillisLoomNetty(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) throws InterruptedException {
        return epochMillis(Approaches.LOOM_NETTY, delayInMillis, delayCallDepth);
    }

    private Long epochMillis(String approach, Long delayInMillis, Integer delayCallDepth) throws InterruptedException {
        log("epochMillisLoom");
        waitOrFetchEpochMillis(approach, delayCallDepth, delayInMillis);
        return System.currentTimeMillis();
    }

    @GetMapping(Approaches.WEBFLUX_NETTY + API_PATH)
    @ResponseBody
    public Mono<Long> epochMillisWebfluxNetty(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) {
        log("epochMillisWebflux");
        return waitOrFetchEpochMillisMono(Approaches.WEBFLUX_NETTY, delayCallDepth, delayInMillis)
                .map(ignore -> System.currentTimeMillis());
    }
}
