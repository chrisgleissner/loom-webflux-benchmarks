package uk.gleissner.loomwebflux.movie;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.movie.repo.MovieRepo;

import java.util.UUID;

import static uk.gleissner.loomwebflux.Approaches.LOOM_NETTY;
import static uk.gleissner.loomwebflux.Approaches.LOOM_TOMCAT;
import static uk.gleissner.loomwebflux.Approaches.PLATFORM_TOMCAT;
import static uk.gleissner.loomwebflux.Approaches.WEBFLUX_NETTY;

@RestController
public class DeleteMovieController extends MovieController {

    DeleteMovieController(Environment environment, MovieRepo movieRepo) {
        super(environment, movieRepo);
    }

    @DeleteMapping(PLATFORM_TOMCAT + API_PATH + "/{id}")
    public void deleteMovieByIdPlatformTomcat(@PathVariable UUID id,
                                              @RequestParam Integer delayCallDepth,
                                              @RequestParam Long delayInMillis) throws InterruptedException {
        deleteMovieById(id, delayCallDepth, delayInMillis, PLATFORM_TOMCAT);
    }

    @DeleteMapping(LOOM_TOMCAT + API_PATH + "/{id}")
    public void deleteMovieByIdLoomTomcat(@PathVariable UUID id,
                                          @RequestParam Integer delayCallDepth,
                                          @RequestParam Long delayInMillis) throws InterruptedException {
        deleteMovieById(id, delayCallDepth, delayInMillis, LOOM_TOMCAT);
    }

    @DeleteMapping(LOOM_NETTY + API_PATH + "/{id}")
    public void deleteMovieByIdLoomNetty(@PathVariable UUID id,
                                         @RequestParam Integer delayCallDepth,
                                         @RequestParam Long delayInMillis) throws InterruptedException {
        deleteMovieById(id, delayCallDepth, delayInMillis, LOOM_NETTY);
    }

    private void deleteMovieById(UUID id, Integer delayCallDepth, Long delayInMillis, String approach) throws InterruptedException {
        log("deleteMoviesById");
        waitOrFetchEpochMillis(approach, delayCallDepth, delayInMillis);
        movieRepo.deleteById(id);
    }

    @DeleteMapping(WEBFLUX_NETTY + API_PATH + "/{id}")
    public Mono<Void> deleteMovieByIdReactive(@PathVariable UUID id,
                                              @RequestParam Integer delayCallDepth,
                                              @RequestParam Long delayInMillis) {
        log("deleteMoviesByIdReactive");
        return waitOrFetchEpochMillisReactive(WEBFLUX_NETTY, delayCallDepth, delayInMillis)
                .then(Mono.fromRunnable(() -> movieRepo.deleteById(id)));
    }
}
