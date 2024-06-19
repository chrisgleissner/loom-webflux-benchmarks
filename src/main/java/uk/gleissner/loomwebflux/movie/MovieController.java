package uk.gleissner.loomwebflux.movie;

import lombok.RequiredArgsConstructor;
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
import uk.gleissner.loomwebflux.movie.domain.Movie;

import java.util.List;
import java.util.Set;

import static uk.gleissner.loomwebflux.common.Approaches.LOOM_NETTY;
import static uk.gleissner.loomwebflux.common.Approaches.LOOM_TOMCAT;
import static uk.gleissner.loomwebflux.common.Approaches.PLATFORM_TOMCAT;
import static uk.gleissner.loomwebflux.common.Approaches.WEBFLUX_NETTY;

@RestController
@RequiredArgsConstructor
public class MovieController {

    private static final String API_PATH = "/movies";
    private final MovieService movieService;

    @GetMapping({PLATFORM_TOMCAT + API_PATH, LOOM_TOMCAT + API_PATH, LOOM_NETTY + API_PATH})
    @ResponseBody
    public Set<Movie> findMoviesByDirectorLastName(@RequestParam String directorLastName,
                                                   @RequestParam Integer delayCallDepth,
                                                   @RequestParam Long delayInMillis) throws InterruptedException {
        return movieService.findMoviesByDirectorLastName(directorLastName, delayCallDepth, delayInMillis);
    }

    @GetMapping(WEBFLUX_NETTY + API_PATH)
    @ResponseBody
    public Flux<Movie> findMoviesByDirectorLastNameReactive(@RequestParam String directorLastName,
                                                            @RequestParam Integer delayCallDepth,
                                                            @RequestParam Long delayInMillis) {
        return movieService.findMoviesByDirectorLastNameReactive(directorLastName, delayCallDepth, delayInMillis);
    }

    @PostMapping({PLATFORM_TOMCAT + API_PATH, LOOM_TOMCAT + API_PATH, LOOM_NETTY + API_PATH})
    public List<Movie> saveMovies(@RequestBody List<Movie> movies,
                                  @RequestParam Integer delayCallDepth,
                                  @RequestParam Long delayInMillis) throws InterruptedException {
        return movieService.saveMovies(movies, delayCallDepth, delayInMillis);
    }

    @PostMapping(WEBFLUX_NETTY + API_PATH)
    public Flux<Movie> saveMoviesReactive(@RequestBody Flux<Movie> movies,
                                          @RequestParam Integer delayCallDepth,
                                          @RequestParam Long delayInMillis) {
        return movieService.saveMoviesReactive(movies, delayCallDepth, delayInMillis);
    }

    @DeleteMapping({PLATFORM_TOMCAT + API_PATH + "/{id}", LOOM_TOMCAT + API_PATH + "/{id}", LOOM_NETTY + API_PATH + "/{id}"})
    public void deleteMovieById(@PathVariable Long id,
                                @RequestParam Integer delayCallDepth,
                                @RequestParam Long delayInMillis) throws InterruptedException {
        movieService.deleteMovieById(id, delayCallDepth, delayInMillis);
    }

    @DeleteMapping(WEBFLUX_NETTY + API_PATH + "/{id}")
    public Mono<Void> deleteMovieByIdReactive(@PathVariable Long id,
                                              @RequestParam Integer delayCallDepth,
                                              @RequestParam Long delayInMillis) {
        return movieService.deleteMovieByIdReactive(id, delayCallDepth, delayInMillis);
    }
}
