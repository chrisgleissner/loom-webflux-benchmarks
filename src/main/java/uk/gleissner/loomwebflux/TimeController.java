package uk.gleissner.loomwebflux;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/epoch-millis")
public class TimeController {

    private static final long DEFAULT_DELAY_MILLIS = 100;

    @GetMapping({"/loom-tomcat", "/loom-netty"})
    @ResponseBody
    public Long epochMillisLoomNetty(@RequestParam(defaultValue = "" + DEFAULT_DELAY_MILLIS) Long delayInMillis) throws InterruptedException {
        Thread.sleep(Duration.ofMillis(delayInMillis));
        return System.currentTimeMillis();
    }

    @GetMapping("/webflux-netty")
    @ResponseBody
    public Mono<Long> epochMillisWebfluxNetty(@RequestParam(defaultValue = "" + DEFAULT_DELAY_MILLIS) Long delayInMillis) {
        return Mono.delay(Duration.ofMillis(delayInMillis))
                .map(d -> System.currentTimeMillis());
    }
}
