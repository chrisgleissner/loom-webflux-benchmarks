package uk.gleissner.loomwebflux.movie;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.config.AppProperties;
import uk.gleissner.loomwebflux.controller.LoomWebFluxController;
import uk.gleissner.loomwebflux.movie.domain.Movie;
import uk.gleissner.loomwebflux.movie.repo.MovieRepo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MovieController implements LoomWebFluxController {

    private static final String API_PATH = "/movies";

    private final AppProperties appProperties;
    private final MovieRepo movieRepo;

    @GetMapping({PLATFORM_TOMCAT + API_PATH, LOOM_TOMCAT + API_PATH, LOOM_NETTY + API_PATH})
    @ResponseBody
    public Set<Movie> findMoviesByDirectorLastNameLoom(@RequestParam String directorLastName,
                                                       @RequestParam(required = false) Long delayInMillis) throws InterruptedException {
        log("findMoviesByDirectorLastNameLoom");
        Thread.sleep(actualDelay(delayInMillis, appProperties));
        return movieRepo.findMoviesByDirector(directorLastName);
    }

    @GetMapping(WEBFLUX_NETTY + API_PATH)
    @ResponseBody
    public Mono<Set<Movie>> findMoviesByDirectorLastNameWebFlux(@RequestParam String directorLastName,
                                                                @RequestParam(required = false) Long delayInMillis) {
        log("findMoviesByDirectorLastNameWebFlux");
        return Mono
                .delay(actualDelay(delayInMillis, appProperties))
                .map(d -> movieRepo.findMoviesByDirector(directorLastName));
    }

    @PostMapping({PLATFORM_TOMCAT + API_PATH, LOOM_TOMCAT + API_PATH, LOOM_NETTY + API_PATH})
    public List<Movie> saveMoviesLoom(@RequestBody List<Movie> movies,
                                      @RequestParam(required = false) Long delayInMillis) throws InterruptedException {
        log("saveMoviesLoom");
        Thread.sleep(actualDelay(delayInMillis, appProperties));
        return movieRepo.saveAll(movies);
    }

    @PostMapping(WEBFLUX_NETTY + API_PATH)
    public Flux<Movie> saveMoviesWebFlux(@RequestBody Flux<Movie> movies,
                                         @RequestParam(required = false) Long delayInMillis) {
        log("saveMoviesWebFlux");
        return movies
                .delaySubscription(actualDelay(delayInMillis, appProperties))
                .flatMap(movie -> Mono.just(movieRepo.save(movie)));
    }

    @DeleteMapping({PLATFORM_TOMCAT + API_PATH + "/{id}", LOOM_TOMCAT + API_PATH + "/{id}", LOOM_NETTY + API_PATH + "/{id}"})
    public void deleteMoviesByIdLoom(@PathVariable UUID id,
                                     @RequestParam(required = false) Long delayInMillis) throws InterruptedException {
        log("deleteMoviesByIdLoom");
        Thread.sleep(actualDelay(delayInMillis, appProperties));
        movieRepo.deleteById(id);
    }

    @DeleteMapping(WEBFLUX_NETTY + API_PATH + "/{id}")
    public Mono<Void> deleteMoviesByIdWebFlux(@PathVariable UUID id,
                                              @RequestParam(required = false) Long delayInMillis) {
        log("deleteMoviesByIdWebFlux");
        return Mono
                .delay(actualDelay(delayInMillis, appProperties))
                .doOnSuccess(d -> movieRepo.deleteById(id))
                .then();
    }

    private void log(String methodName) {
        if (log.isDebugEnabled()) {
            log.debug("{}: thread={}", methodName, Thread.currentThread());
        }
    }
}
