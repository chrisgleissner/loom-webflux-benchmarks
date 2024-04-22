package uk.gleissner.loomwebflux.movie.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@Jacksonized
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Movie implements Comparable<Movie> {

    private final static Comparator<Movie> comparator = Comparator.comparing(Movie::getTitle)
            .thenComparingInt(Movie::getReleaseYear)
            .thenComparing(Movie::getGenre)
            .thenComparing(Movie::getId);

    @EqualsAndHashCode.Include
    UUID id;

    @NonNull
    String title;

    @NonNull
    Integer releaseYear;

    @NonNull
    Genre genre;

    List<Actor> actors;
    List<Person> directors;
    List<Person> writers;
    List<Award> awards;
    Double rating;

    @Override
    public int compareTo(@NotNull Movie o) {
        return comparator.compare(this, o);
    }
}





