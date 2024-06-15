package uk.gleissner.loomwebflux.movie.repo;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.ListCrudRepository;
import uk.gleissner.loomwebflux.movie.domain.Movie;

import java.util.Set;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;

public interface MovieRepo extends ListCrudRepository<Movie, Long> {

    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    @Query("SELECT m FROM Movie m JOIN m.directors d WHERE d.lastName = :directorName")
    Set<Movie> findByDirectorName(String directorName);
}
