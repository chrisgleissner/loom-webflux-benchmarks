package uk.gleissner.loomwebflux.movie.repo;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gleissner.loomwebflux.config.AppProperties;
import uk.gleissner.loomwebflux.movie.domain.Movie;

import java.util.Set;

@Component
public class AppPropertiesAwareMovieRepo {

    private static final String MOVIES_BY_DIRECTOR_NAME_CACHE_NAME = "moviesByDirectorName";

    private final AppProperties appProperties;
    private final MovieRepo underlying;
    private final Cache moviesByDirectorNameCache;

    AppPropertiesAwareMovieRepo(AppProperties appProperties, MovieRepo underlying, CacheManager cacheManager) {
        this.appProperties = appProperties;
        this.underlying = underlying;
        this.moviesByDirectorNameCache = cacheManager.getCache(MOVIES_BY_DIRECTOR_NAME_CACHE_NAME);
    }

    @Cacheable("moviesByDirectorName")
    public Set<Movie> findByDirectorName(String directorName) {
        return underlying.findByDirectorName(directorName);
    }

    public Movie save(Movie movie) {
        if (appProperties.repoReadOnly()) {
            return movie;
        } else {
            // TODO cg Save new movie to cache
            moviesByDirectorNameCache.invalidate();
            return underlying.save(movie);
        }
    }

    public void deleteById(Long id) {
        if (!appProperties.repoReadOnly()) {
            underlying.deleteById(id);
            // TODO cg Expire deleted movie from cache
            moviesByDirectorNameCache.invalidate();
        }
    }
}
