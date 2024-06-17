package uk.gleissner.loomwebflux.movie.repo;

import com.google.common.collect.Lists;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gleissner.loomwebflux.config.AppProperties;
import uk.gleissner.loomwebflux.movie.domain.Movie;

import java.util.List;
import java.util.Set;

@Component
public class CachedMovieRepo {

    private static final String MOVIES_BY_DIRECTOR_NAME_CACHE_NAME = "moviesByDirectorName";

    private final AppProperties appProperties;
    private final MovieRepo underlying;
    private final Cache moviesByDirectorNameCache;

    CachedMovieRepo(AppProperties appProperties, MovieRepo underlying, CacheManager cacheManager) {
        this.appProperties = appProperties;
        this.underlying = underlying;
        this.moviesByDirectorNameCache = cacheManager.getCache(MOVIES_BY_DIRECTOR_NAME_CACHE_NAME);
    }

    @Cacheable(MOVIES_BY_DIRECTOR_NAME_CACHE_NAME)
    public Set<Movie> findByDirectorName(String directorName) {
        return underlying.findByDirectorName(directorName);
    }
    
    public List<Movie> saveAll(Iterable<Movie> movies) {
        if (appProperties.repoReadOnly()) {
            return Lists.newArrayList(movies);
        } else {
            movies.forEach(this::evictMovieFromCache);
            return underlying.saveAll(movies);
        }
    }

    public void deleteById(Long id) {
        if (!appProperties.repoReadOnly()) {
            underlying.findById(id).ifPresent(this::evictMovieFromCache);
            underlying.deleteById(id);
        }
    }

    private void evictMovieFromCache(Movie movie) {
        movie.getDirectors().forEach(director -> moviesByDirectorNameCache.evict(director.getLastName()));
    }
}
