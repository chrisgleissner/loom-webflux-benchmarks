package uk.gleissner.loomwebflux.movie.repo;

import uk.gleissner.loomwebflux.movie.domain.Movie;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MovieRepo {

    Set<Movie> findMoviesByDirector(String directorName);

    Movie save(Movie movie);

    default List<Movie> saveAll(List<Movie> movies) {
        return movies.stream().map(this::save).toList();
    }

    void deleteById(UUID id);
}
