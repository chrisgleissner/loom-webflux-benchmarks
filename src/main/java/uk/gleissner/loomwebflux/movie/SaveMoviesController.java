package uk.gleissner.loomwebflux.movie;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.movie.domain.Movie;
import uk.gleissner.loomwebflux.movie.repo.MovieRepo;

import java.util.List;

import static uk.gleissner.loomwebflux.Approaches.LOOM_NETTY;
import static uk.gleissner.loomwebflux.Approaches.LOOM_TOMCAT;
import static uk.gleissner.loomwebflux.Approaches.PLATFORM_TOMCAT;
import static uk.gleissner.loomwebflux.Approaches.WEBFLUX_NETTY;

@RestController
public class SaveMoviesController extends MovieController {

    SaveMoviesController(Environment environment, MovieRepo movieRepo) {
        super(environment, movieRepo);
    }

    @PostMapping(PLATFORM_TOMCAT + API_PATH)
    public List<Movie> saveMoviesPlatformTomcat(@RequestBody List<Movie> movies,
                                                @RequestParam Integer delayCallDepth,
                                                @RequestParam Long delayInMillis) throws InterruptedException {
        return saveMovies(movies, delayCallDepth, delayInMillis, PLATFORM_TOMCAT);
    }

    @PostMapping(LOOM_TOMCAT + API_PATH)
    public List<Movie> saveMoviesLoomTomcat(@RequestBody List<Movie> movies,
                                            @RequestParam Integer delayCallDepth,
                                            @RequestParam Long delayInMillis) throws InterruptedException {
        return saveMovies(movies, delayCallDepth, delayInMillis, LOOM_TOMCAT);
    }

    @PostMapping(LOOM_NETTY + API_PATH)
    public List<Movie> saveMoviesLoomNetty(@RequestBody List<Movie> movies,
                                           @RequestParam Integer delayCallDepth,
                                           @RequestParam Long delayInMillis) throws InterruptedException {
        return saveMovies(movies, delayCallDepth, delayInMillis, LOOM_NETTY);
    }

    private List<Movie> saveMovies(List<Movie> movies, Integer delayCallDepth, Long delayInMillis, String approach) throws InterruptedException {
        log("saveMovies");
        waitOrFetchEpochMillis(approach, delayCallDepth, delayInMillis);
        return movieRepo.saveAll(movies);
    }

    @PostMapping(WEBFLUX_NETTY + API_PATH)
    public Flux<Movie> saveMoviesReactive(@RequestBody Flux<Movie> movies,
                                          @RequestParam Integer delayCallDepth,
                                          @RequestParam Long delayInMillis) {
        log("saveMoviesReactive");
        return waitOrFetchEpochMillisReactive(WEBFLUX_NETTY, delayCallDepth, delayInMillis)
                .flatMapMany(ignore -> movies.flatMap(movie -> Mono.just(movieRepo.save(movie))));
    }
}
