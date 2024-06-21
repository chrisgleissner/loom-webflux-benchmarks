package uk.gleissner.loomwebflux.movie.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
// TODO cg Needed by MongoDB but fails JPA tests
//@Document
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Movie implements Comparable<Movie> {

    private final static Comparator<Movie> comparator = Comparator.comparing(Movie::getTitle)
        .thenComparingInt(Movie::getReleaseYear)
        .thenComparing(Movie::getGenre)
        .thenComparing(Movie::getId);

    // TODO cg This is not usable by MongoDB and currently fails with
    //    org.springframework.dao.InvalidDataAccessApiUsageException: Cannot autogenerate id of type java.lang.Long for entity
    //    of type uk.gleissner.loomwebflux.movie.domain.Movie
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @EqualsAndHashCode.Exclude
    Long id;

    @NonNull
    String title;

    @NonNull
    Integer releaseYear;

    @NonNull
    Genre genre;

    @ManyToMany(cascade = ALL, fetch = EAGER)
    List<Character> characters;

    @ManyToMany(cascade = PERSIST, fetch = EAGER)
    List<Person> directors;

    @ManyToMany(cascade = PERSIST, fetch = EAGER)
    List<Person> writers;

    @ManyToMany(cascade = ALL, fetch = EAGER)
    List<Award> awards;

    Double rating;

    @Override
    public int compareTo(@NotNull Movie o) {
        return comparator.compare(this, o);
    }
}





