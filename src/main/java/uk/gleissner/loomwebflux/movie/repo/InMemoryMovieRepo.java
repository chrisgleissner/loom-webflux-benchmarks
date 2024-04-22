package uk.gleissner.loomwebflux.movie.repo;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gleissner.loomwebflux.config.AppProperties;
import uk.gleissner.loomwebflux.movie.domain.Actor;
import uk.gleissner.loomwebflux.movie.domain.Award;
import uk.gleissner.loomwebflux.movie.domain.Genre;
import uk.gleissner.loomwebflux.movie.domain.Movie;
import uk.gleissner.loomwebflux.movie.domain.Person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.util.UUID.randomUUID;

@Component
@RequiredArgsConstructor
public class InMemoryMovieRepo implements MovieRepo {

    private final AppProperties appProperties;
    private final Map<UUID, Movie> movieById = new ConcurrentHashMap<>();
    private final Map<String, Set<Movie>> moviesByDirectorLastName = new ConcurrentHashMap<>();

    @PostConstruct
    void setupMockData() {
        sampleMovies().forEach(m -> this.save(m, true));
    }

    @Override
    public Optional<Movie> findMovieById(UUID id) {
        return Optional.ofNullable(movieById.get(id));
    }

    @Override
    public Set<Movie> findMoviesByDirector(String directorLastName) {
        return moviesByDirectorLastName.get(directorLastName.toLowerCase());
    }

    @Override
    public Movie save(Movie movie) {
        return save(movie, false);
    }

    private Movie save(Movie movie, boolean postConstruct) {
        val movieToSave = movie.getId() == null ? movie.toBuilder().id(randomUUID()).build() : movie;
        if (postConstruct || !appProperties.repoReadOnly()) {
            for (val director : movieToSave.getDirectors()) {
                movieById.put(movieToSave.getId(), movieToSave);
                moviesByDirectorLastName.computeIfAbsent(director.getLastName().toLowerCase(), k -> new ConcurrentSkipListSet<>()).add(movieToSave);
            }
        }
        return movieToSave;
    }

    @Override
    public void deleteById(UUID id) {
        if (!appProperties.repoReadOnly()) {
            Optional.ofNullable(movieById.remove(id)).ifPresent(movie ->
                    movie.getDirectors().forEach(d -> moviesByDirectorLastName.get(d.getLastName().toLowerCase()).remove(movie)));
        }
    }

    private static List<Movie> sampleMovies() {
        val movies = new ArrayList<Movie>();

        // Alfred Hitchcock movies
        val hitchcock = Person.of("Alfred Hitchcock", LocalDate.of(1899, 8, 13));
        movies.add(Movie.builder()
                .title("Rear Window")
                .releaseYear(1954)
                .actors(List.of(
                        Actor.builder().person(Person.of("James Stewart")).role("L.B. Jefferies").build(),
                        Actor.builder().person(Person.of("Grace Kelly")).role("Lisa Carol Fremont").build()
                ))
                .directors(List.of(hitchcock))
                .writers(List.of(Person.of("Joseph Stefano")))
                .awards(List.of(Award.builder().name("Golden Globe").year(1955).build()))
                .genre(Genre.MYSTERY)
                .rating(8.5)
                .build());

        movies.add(Movie.builder()
                .title("Vertigo")
                .releaseYear(1958)
                .actors(List.of(
                        Actor.builder().person(Person.of("James Stewart")).role("John 'Scottie' Ferguson").build(),
                        Actor.builder().person(Person.of("Kim Novak")).role("Madeleine Elster").build()
                ))
                .directors(List.of(hitchcock))
                .writers(List.of(Person.of("Alec Coppel")))
                .awards(List.of(Award.builder().name("National Film Registry").year(1989).build()))
                .genre(Genre.MYSTERY)
                .rating(8.3)
                .build());

        movies.add(Movie.builder()
                .title("North by Northwest")
                .releaseYear(1959)
                .actors(List.of(
                        Actor.builder().person(Person.of("Cary Grant")).role("Roger O. Thornhill").build(),
                        Actor.builder().person(Person.of("Eva Marie Saint")).role("Eve Kendall").build()
                ))
                .directors(List.of(hitchcock))
                .writers(List.of(Person.of("Ernest Lehman")))
                .awards(List.of(Award.builder().name("Academy Award").year(1960).build()))
                .genre(Genre.ACTION)
                .rating(8.4)
                .build());

        // Stanley Kubrick movies
        val kubrick = Person.of("Stanley Kubrick", LocalDate.of(1928, 7, 26));
        movies.add(Movie.builder()
                .title("2001: A Space Odyssey")
                .releaseYear(1968)
                .actors(List.of(
                        Actor.builder().person(Person.of("Keir Dullea")).role("Dr. Dave Bowman").build(),
                        Actor.builder().person(Person.of("Gary Lockwood")).role("Dr. Frank Poole").build()
                ))
                .directors(List.of(kubrick))
                .writers(List.of(kubrick))
                .awards(List.of(Award.builder().name("Academy Award").year(1969).build()))
                .genre(Genre.SCIENCE_FICTION)
                .rating(8.7)
                .build());

        movies.add(Movie.builder()
                .title("Dr. Strangelove or: How I Learned to Stop Worrying and Love the Bomb")
                .releaseYear(1964)
                .actors(List.of(
                        Actor.builder().person(Person.of("Peter Sellers")).role("Group Capt. Lionel Mandrake / President Merkin Muffley / Dr. Strangelove").build(),
                        Actor.builder().person(Person.of("George C. Scott")).role("Gen. 'Buck' Turgidson").build()
                ))
                .directors(List.of(kubrick))
                .writers(List.of(kubrick))
                .awards(List.of(Award.builder().name("BAFTA Award").year(1965).build()))
                .genre(Genre.COMEDY)
                .rating(8.4)
                .build());

        // Woody Allen movies
        val allen = Person.of("Woody Allen", LocalDate.of(1935, 11, 30));
        movies.add(Movie.builder()
                .title("Annie Hall")
                .releaseYear(1977)
                .actors(List.of(
                        Actor.builder().person(allen).role("Alvy Singer").build(),
                        Actor.builder().person(Person.of("Diane Keaton")).role("Annie Hall").build()
                ))
                .directors(List.of(allen))
                .writers(List.of(allen))
                .awards(List.of(
                        Award.builder().name("Academy Award").year(1978).build(),
                        Award.builder().name("Golden Globe").year(1978).build()
                ))
                .genre(Genre.ROMANCE)
                .rating(8.0)
                .build());

        movies.add(Movie.builder()
                .title("Manhattan")
                .releaseYear(1979)
                .actors(List.of(
                        Actor.builder().person(allen).role("Isaac Davis").build(),
                        Actor.builder().person(Person.of("Diane Keaton")).role("Mary Wilkie").build()
                ))
                .directors(List.of(allen))
                .writers(List.of(allen))
                .awards(List.of(Award.builder().name("BAFTA Award").year(1980).build()))
                .genre(Genre.DRAMA)
                .rating(7.9)
                .build());

        movies.add(Movie.builder()
                .title("Zelig")
                .releaseYear(1983)
                .actors(List.of(
                        Actor.builder().person(allen).role("Leonard Zelig").build(),
                        Actor.builder().person(Person.of("Mia Farrow")).role("Dr. Eudora Nesbitt Fletcher").build()
                ))
                .directors(List.of(allen))
                .writers(List.of(allen))
                .awards(List.of(Award.builder().name("Golden Globe").year(1984).build()))
                .genre(Genre.COMEDY)
                .rating(7.8)
                .build());

        return movies;
    }
}
