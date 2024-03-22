package uk.gleissner.loomwebflux;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/epoch-millis")
public class TimeController {

    private static final long DEFAULT_DELAY_MILLIS = 100;

    // Loom
    @GetMapping("/loom")
    @ResponseBody
    public Long epochMillisLoom(@RequestParam(required = false, defaultValue = "" + DEFAULT_DELAY_MILLIS) Long delayMillis) throws InterruptedException {
        Thread.sleep(delayMillis);
        return System.currentTimeMillis();
    }

    // Webflux
    @GetMapping("/webflux")
    @ResponseBody
    public Mono<Long> epochMillisWebflux(@RequestParam(required = false, defaultValue = "" + DEFAULT_DELAY_MILLIS) Long delayMillis) {
        return Mono.just(System.currentTimeMillis())
                .delayElement(Duration.ofMillis(delayMillis));
    }
}
