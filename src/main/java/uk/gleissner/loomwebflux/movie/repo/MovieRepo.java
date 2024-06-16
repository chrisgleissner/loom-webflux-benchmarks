package uk.gleissner.loomwebflux.movie.repo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import uk.gleissner.loomwebflux.movie.domain.Movie;

import java.util.Set;

public interface MovieRepo extends ListCrudRepository<Movie, Long> {

    @Query("SELECT m FROM Movie m JOIN m.directors d WHERE d.lastName = :directorName")
    Set<Movie> findByDirectorName(String directorName);
}
