package uk.gleissner.loomwebflux;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/epoch-millis")
public class TimeController {

    private static final long DEFAULT_DELAY_MILLIS = 100;

    @GetMapping("/loom")
    @ResponseBody
    public Long epochMillisLoom(@RequestParam(defaultValue = "" + DEFAULT_DELAY_MILLIS) Long delayMillis) throws InterruptedException {
        Thread.sleep(Duration.ofMillis(delayMillis));
        return System.currentTimeMillis();
    }

    @GetMapping("/webflux")
    @ResponseBody
    public Mono<Long> epochMillisWebflux(@RequestParam(defaultValue = "" + DEFAULT_DELAY_MILLIS) Long delayMillis) {
        return Mono.delay(Duration.ofMillis(delayMillis))
                .map(d -> System.currentTimeMillis());
    }
}
