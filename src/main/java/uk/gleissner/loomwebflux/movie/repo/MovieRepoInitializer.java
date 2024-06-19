package uk.gleissner.loomwebflux.movie.repo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gleissner.loomwebflux.movie.domain.Award;
import uk.gleissner.loomwebflux.movie.domain.Character;
import uk.gleissner.loomwebflux.movie.domain.Genre;
import uk.gleissner.loomwebflux.movie.domain.Movie;
import uk.gleissner.loomwebflux.movie.domain.Person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
class MovieRepoInitializer {

    private final MovieRepo movieRepo;

    @EventListener(ApplicationReadyEvent.class)
    void initMovieRepo() {
        val movies = new ArrayList<Movie>();

        // Alfred Hitchcock movies
        val hitchcock = Person.of("Alfred Hitchcock", LocalDate.of(1899, 8, 13));
        movies.add(Movie.builder()
            .title("Rear Window")
            .releaseYear(1954)
            .characters(List.of(
                Character.builder().actor(Person.of("James Stewart")).name("L.B. Jefferies").build(),
                Character.builder().actor(Person.of("Grace Kelly")).name("Lisa Carol Fremont").build()
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
            .characters(List.of(
                Character.builder().actor(Person.of("James Stewart")).name("John 'Scottie' Ferguson").build(),
                Character.builder().actor(Person.of("Kim Novak")).name("Madeleine Elster").build()
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
            .characters(List.of(
                Character.builder().actor(Person.of("Cary Grant")).name("Roger O. Thornhill").build(),
                Character.builder().actor(Person.of("Eva Marie Saint")).name("Eve Kendall").build()
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
            .characters(List.of(
                Character.builder().actor(Person.of("Keir Dullea")).name("Dr. Dave Bowman").build(),
                Character.builder().actor(Person.of("Gary Lockwood")).name("Dr. Frank Poole").build()
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
            .characters(List.of(
                Character.builder().actor(Person.of("Peter Sellers")).name("Group Capt. Lionel Mandrake / President Merkin Muffley / Dr. Strangelove").build(),
                Character.builder().actor(Person.of("George C. Scott")).name("Gen. 'Buck' Turgidson").build()
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
            .characters(List.of(
                Character.builder().actor(allen).name("Alvy Singer").build(),
                Character.builder().actor(Person.of("Diane Keaton")).name("Annie Hall").build()
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
            .characters(List.of(
                Character.builder().actor(allen).name("Isaac Davis").build(),
                Character.builder().actor(Person.of("Diane Keaton")).name("Mary Wilkie").build()
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
            .characters(List.of(
                Character.builder().actor(allen).name("Leonard Zelig").build(),
                Character.builder().actor(Person.of("Mia Farrow")).name("Dr. Eudora Nesbitt Fletcher").build()
            ))
            .directors(List.of(allen))
            .writers(List.of(allen))
            .awards(List.of(Award.builder().name("Golden Globe").year(1984).build()))
            .genre(Genre.COMEDY)
            .rating(7.8)
            .build());

        movieRepo.saveAll(movies);
    }
}
