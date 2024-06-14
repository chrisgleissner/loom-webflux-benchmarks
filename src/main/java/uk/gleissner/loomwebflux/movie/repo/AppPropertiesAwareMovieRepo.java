package uk.gleissner.loomwebflux.movie.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gleissner.loomwebflux.config.AppProperties;
import uk.gleissner.loomwebflux.movie.domain.Movie;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AppPropertiesAwareMovieRepo {
    private final AppProperties appProperties;
    private final MovieRepo underlying;

    public Set<Movie> findByDirectorName(String directorName) {
        return underlying.findByDirectorName(directorName);
    }

    public Movie save(Movie movie) {
        return appProperties.repoReadOnly() ? movie : underlying.save(movie);
    }

    public void deleteById(Long id) {
        if (!appProperties.repoReadOnly())
            underlying.deleteById(id);
    }
}
