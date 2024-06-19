package uk.gleissner.loomwebflux.time;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static uk.gleissner.loomwebflux.common.Approaches.LOOM_NETTY;
import static uk.gleissner.loomwebflux.common.Approaches.LOOM_TOMCAT;
import static uk.gleissner.loomwebflux.common.Approaches.PLATFORM_TOMCAT;
import static uk.gleissner.loomwebflux.common.Approaches.WEBFLUX_NETTY;

@RestController
@RequiredArgsConstructor
public class TimeController {

    public static final String REACTIVE = "reactive";
    public static final String NON_REACTIVE = "non-reactive";
    public static final String API_PATH = "/epoch-millis";

    private final TimeService timeService;

    @GetMapping({NON_REACTIVE + API_PATH, PLATFORM_TOMCAT + API_PATH, LOOM_TOMCAT + API_PATH, LOOM_NETTY + API_PATH})
    @ResponseBody
    public Long epochMillis(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) throws InterruptedException {
        return timeService.epochMillis(delayInMillis, delayCallDepth);
    }

    @GetMapping({REACTIVE + API_PATH, WEBFLUX_NETTY + API_PATH})
    @ResponseBody
    public Mono<Long> epochMillisReactive(@RequestParam Long delayInMillis, @RequestParam Integer delayCallDepth) {
        return timeService.epochMillisReactive(delayInMillis, delayCallDepth);
    }
}
