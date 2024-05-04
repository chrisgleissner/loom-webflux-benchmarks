package uk.gleissner.loomwebflux.movie;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import uk.gleissner.loomwebflux.movie.domain.Movie;
import uk.gleissner.loomwebflux.movie.repo.MovieRepo;

import java.util.Set;

import static uk.gleissner.loomwebflux.Approaches.LOOM_NETTY;
import static uk.gleissner.loomwebflux.Approaches.LOOM_TOMCAT;
import static uk.gleissner.loomwebflux.Approaches.PLATFORM_TOMCAT;
import static uk.gleissner.loomwebflux.Approaches.WEBFLUX_NETTY;

@RestController
public class FindMoviesController extends MovieController {

    FindMoviesController(Environment environment, MovieRepo movieRepo) {
        super(environment, movieRepo);
    }

    @GetMapping(PLATFORM_TOMCAT + API_PATH)
    @ResponseBody
    public Set<Movie> findMoviesByDirectorLastNamePlatformTomcat(@RequestParam String directorLastName,
                                                                 @RequestParam Long delayInMillis,
                                                                 @RequestParam Integer delayCallDepth) throws InterruptedException {
        return findMoviesByDirectorLastName(directorLastName, delayInMillis, delayCallDepth, PLATFORM_TOMCAT);
    }

    @GetMapping(LOOM_TOMCAT + API_PATH)
    @ResponseBody
    public Set<Movie> findMoviesByDirectorLastNameLoomTomcat(@RequestParam String directorLastName,
                                                             @RequestParam Long delayInMillis,
                                                             @RequestParam Integer delayCallDepth) throws InterruptedException {
        return findMoviesByDirectorLastName(directorLastName, delayInMillis, delayCallDepth, LOOM_TOMCAT);
    }

    @GetMapping(LOOM_NETTY + API_PATH)
    @ResponseBody
    public Set<Movie> findMoviesByDirectorLastNameLoomNetty(@RequestParam String directorLastName,
                                                            @RequestParam Long delayInMillis,
                                                            @RequestParam Integer delayCallDepth) throws InterruptedException {
        return findMoviesByDirectorLastName(directorLastName, delayInMillis, delayCallDepth, LOOM_NETTY);
    }

    private Set<Movie> findMoviesByDirectorLastName(String directorLastName, Long delayInMillis, Integer delayCallDepth, String approach) throws InterruptedException {
        log("findMoviesByDirectorLastName");
        waitOrFetchEpochMillis(approach, delayCallDepth, delayInMillis);
        return movieRepo.findMoviesByDirector(directorLastName);
    }

    @GetMapping(WEBFLUX_NETTY + API_PATH)
    @ResponseBody
    public Flux<Movie> findMoviesByDirectorLastNameReactive(@RequestParam String directorLastName,
                                                            @RequestParam Long delayInMillis,
                                                            @RequestParam(defaultValue = "0") Integer delayCallDepth) {
        log("findMoviesByDirectorLastNameReactive");
        return waitOrFetchEpochMillisMono(WEBFLUX_NETTY, delayCallDepth, delayInMillis)
                .thenMany(Flux.defer(() -> Flux.fromIterable(movieRepo.findMoviesByDirector(directorLastName))));
    }
}
