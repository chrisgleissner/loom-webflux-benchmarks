package uk.gleissner.loomwebflux.movie;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gleissner.loomwebflux.common.AbstractService;
import uk.gleissner.loomwebflux.movie.domain.Movie;
import uk.gleissner.loomwebflux.movie.repo.CachedMovieRepo;

import java.util.List;
import java.util.Set;

import static reactor.core.scheduler.Schedulers.boundedElastic;

@Service
public class MovieService extends AbstractService {

    private final CachedMovieRepo movieRepo;

    MovieService(WebClient webClient, CachedMovieRepo movieRepo) {
        super(webClient);
        this.movieRepo = movieRepo;
    }

    public Set<Movie> findMoviesByDirectorLastName(String directorLastName, Integer delayCallDepth, Long delayInMillis) throws InterruptedException {
        log("findMoviesByDirectorLastName");
        waitOrFetchEpochMillis(delayCallDepth, delayInMillis);
        return movieRepo.findByDirectorName(directorLastName);
    }

    public Flux<Movie> findMoviesByDirectorLastNameReactive(String directorLastName, Integer delayCallDepth, Long delayInMillis) {
        log("findMoviesByDirectorLastNameReactive");
        return waitOrFetchEpochMillisReactive(delayCallDepth, delayInMillis)
            .thenMany(Flux.defer(() -> Flux.fromIterable(movieRepo.findByDirectorName(directorLastName))));
    }

    public List<Movie> saveMovies(List<Movie> movies, Integer delayCallDepth, @RequestParam Long delayInMillis) throws InterruptedException {
        log("saveMovies");
        waitOrFetchEpochMillis(delayCallDepth, delayInMillis);
        return movieRepo.saveAll(movies);
    }

    public Flux<Movie> saveMoviesReactive(Flux<Movie> movies, Integer delayCallDepth, Long delayInMillis) {
        log("saveMoviesReactive");
        return waitOrFetchEpochMillisReactive(delayCallDepth, delayInMillis)
            .flatMapMany(ignore -> movies
                .publishOn(boundedElastic())
                .collectList()
                .flatMapMany(movieList -> Flux.fromIterable(movieRepo.saveAll(movieList))));
    }

    public void deleteMovieById(Long id, Integer delayCallDepth, Long delayInMillis) throws InterruptedException {
        log("deleteMoviesById");
        waitOrFetchEpochMillis(delayCallDepth, delayInMillis);
        movieRepo.deleteById(id);
    }

    public Mono<Void> deleteMovieByIdReactive(Long id, Integer delayCallDepth, Long delayInMillis) {
        log("deleteMovieByIdReactive");
        return waitOrFetchEpochMillisReactive(delayCallDepth, delayInMillis)
            .then(Mono.fromRunnable(() -> movieRepo.deleteById(id))
                .subscribeOn(boundedElastic()))
            .then();
    }

}
