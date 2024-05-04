package uk.gleissner.loomwebflux.movie;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.controller.LoomWebFluxController;
import uk.gleissner.loomwebflux.movie.domain.Movie;
import uk.gleissner.loomwebflux.movie.repo.MovieRepo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MovieController extends LoomWebFluxController {

    private static final String API_PATH = "/movies";

    private final MovieRepo movieRepo;
    private final Environment environment;

    @Getter(lazy = true)
    private final WebClient webClient = WebClient.create("http://localhost:" + environment.getProperty("local.server.port"));

    @GetMapping({PLATFORM_TOMCAT + API_PATH, LOOM_TOMCAT + API_PATH, LOOM_NETTY + API_PATH})
    @ResponseBody
    public Set<Movie> findMoviesByDirectorLastNameLoom(@RequestParam String directorLastName,
                                                       @RequestParam String approach,
                                                       @RequestParam Long delayInMillis,
                                                       @RequestParam(defaultValue = "0") Integer delayCallDepth) throws InterruptedException {
        log("findMoviesByDirectorLastNameLoom");
        waitOrFetchEpochMillis(getWebClient(), approach, delayCallDepth, delayInMillis);
        return movieRepo.findMoviesByDirector(directorLastName);
    }

    @GetMapping(WEBFLUX_NETTY + API_PATH)
    @ResponseBody
    public Flux<Movie> findMoviesByDirectorLastNameWebFlux(@RequestParam String directorLastName,
                                                           @RequestParam String approach,
                                                           @RequestParam Long delayInMillis,
                                                           @RequestParam(defaultValue = "0") Integer delayCallDepth) {
        log("findMoviesByDirectorLastNameWebFlux");
        return waitOrFetchEpochMillisMono(getWebClient(), approach, delayCallDepth, delayInMillis)
                .thenMany(Flux.defer(() -> Flux.fromIterable(movieRepo.findMoviesByDirector(directorLastName))));
    }

    @PostMapping({PLATFORM_TOMCAT + API_PATH, LOOM_TOMCAT + API_PATH, LOOM_NETTY + API_PATH})
    public List<Movie> saveMoviesLoom(@RequestBody List<Movie> movies,
                                      @RequestParam String approach,
                                      @RequestParam Long delayInMillis,
                                      @RequestParam(defaultValue = "0") Integer delayCallDepth) throws InterruptedException {
        log("saveMoviesLoom");
        waitOrFetchEpochMillis(getWebClient(), approach, delayCallDepth, delayInMillis);
        return movieRepo.saveAll(movies);
    }

    @PostMapping(WEBFLUX_NETTY + API_PATH)
    public Flux<Movie> saveMoviesWebFlux(ServerHttpRequest request,
                                         @RequestBody Flux<Movie> movies,
                                         @RequestParam String approach,
                                         @RequestParam Long delayInMillis,
                                         @RequestParam(defaultValue = "0") Integer delayCallDepth) {
        log("saveMoviesWebFlux");
        return waitOrFetchEpochMillisMono(getWebClient(), approach, delayCallDepth, delayInMillis)
                .flatMapMany(ignore -> movies.flatMap(movie -> Mono.just(movieRepo.save(movie))));
    }

    @DeleteMapping({PLATFORM_TOMCAT + API_PATH + "/{id}", LOOM_TOMCAT + API_PATH + "/{id}", LOOM_NETTY + API_PATH + "/{id}"})
    public void deleteMoviesByIdLoom(@PathVariable UUID id,
                                     @RequestParam String approach,
                                     @RequestParam Long delayInMillis,
                                     @RequestParam(defaultValue = "0") Integer delayCallDepth) throws InterruptedException {
        log("deleteMoviesByIdLoom");
        waitOrFetchEpochMillis(getWebClient(), approach, delayCallDepth, delayInMillis);
        movieRepo.deleteById(id);
    }

    @DeleteMapping(WEBFLUX_NETTY + API_PATH + "/{id}")
    public Mono<Void> deleteMoviesByIdWebFlux(@PathVariable UUID id,
                                              @RequestParam String approach,
                                              @RequestParam Long delayInMillis,
                                              @RequestParam(defaultValue = "0") Integer delayCallDepth) {
        log("deleteMoviesByIdWebFlux");
        return waitOrFetchEpochMillisMono(getWebClient(), approach, delayCallDepth, delayInMillis)
                .then(Mono.fromRunnable(() -> movieRepo.deleteById(id)));
    }
}
