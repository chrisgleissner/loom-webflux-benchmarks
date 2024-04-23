package uk.gleissner.loomwebflux.movie

import nl.altindag.log.LogCaptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.BodyInserters
import uk.gleissner.loomwebflux.config.AppProperties
import uk.gleissner.loomwebflux.controller.LoomWebFluxController.*
import uk.gleissner.loomwebflux.fixture.AbstractIntegrationTest
import uk.gleissner.loomwebflux.movie.domain.*
import uk.gleissner.loomwebflux.movie.repo.MovieRepo
import java.time.Duration
import java.time.Instant.now
import java.time.LocalDate
import java.util.*

internal class MovieControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var movieRepo: MovieRepo

    @Autowired
    private lateinit var appProperties: AppProperties

    @ParameterizedTest
    @ValueSource(strings = [LOOM_TOMCAT, LOOM_NETTY, WEBFLUX_NETTY])
    fun `find movies by director last name`(approach: String) {
        val logCaptor = logCaptor()
        val movies = getMovies(approach)
        assertThat(movies).containsExactlyElementsOf(movieRepo.findMoviesByDirector("Allen"))
        logCaptor.assertCorrectThreadType(approach)
    }

    @ParameterizedTest
    @ValueSource(strings = [LOOM_TOMCAT, LOOM_NETTY, WEBFLUX_NETTY])
    fun `save and delete a movie`(approach: String) {
        val logCaptor = logCaptor()

        val moviesByDavidLynch = listOf(mulhollandDrive, theStraightStory)
        moviesByDavidLynch.forEach {
            assertThat(it.id).isNull()
        }

        val movies = getMovies(approach, davidLynch.lastName)
        assertThat(movies).isEmpty()

        val savedMovies = saveMovies(approach, moviesByDavidLynch)
        assertThat(savedMovies).hasSize(moviesByDavidLynch.size)
        savedMovies.forEach {
            assertThat(it.id).isNotNull()
        }
        assertThat(savedMovies).usingRecursiveComparison().ignoringFields("id").isEqualTo(moviesByDavidLynch)

        savedMovies.forEach {
            deleteMovie(approach, it.id)
        }
        assertThat(getMovies(approach, davidLynch.lastName)).doesNotContainAnyElementsOf(savedMovies)

        logCaptor.assertCorrectThreadType(approach)
    }

    private fun logCaptor() = LogCaptor.forClass(MovieController::class.java)

    private fun LogCaptor.assertCorrectThreadType(approach: String) {
        val expectedThreadNameFragment =
            if (approach == LOOM_TOMCAT || approach == LOOM_NETTY) "thread=VirtualThread" else "thread=Thread"
        assertThat(debugLogs.filter { it.contains("thread=") }).allSatisfy {
            assertThat(it).contains(expectedThreadNameFragment)
        }
    }

    private fun getMovies(approach: String, directorLastName: String = "Allen"): MutableList<Movie>? {
        val start = now()
        val movies = client.get().uri("$approach/movies?directorLastName=$directorLastName").exchange()
            .expectStatus().isOk()
            .expectBodyList(Movie::class.java).returnResult().responseBody
        assertThat(Duration.between(start, now())).isGreaterThan(appProperties.defaultDelay)
        return movies
    }

    private fun saveMovies(approach: String, movies: List<Movie>): List<Movie> {
        val start = now()
        val savedMovies = client.post().uri("$approach/movies")
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON)
            .body(
                BodyInserters.fromValue(movies)
            ).exchange().expectStatus().isOk
            .expectBodyList(Movie::class.java).returnResult().responseBody?.toList() ?: listOf()
        assertThat(Duration.between(start, now())).isGreaterThan(appProperties.defaultDelay)
        return savedMovies
    }

    private fun deleteMovie(approach: String, movieId: UUID) {
        val start = now()
        client.delete().uri("$approach/movies/${movieId}")
            .exchange().expectStatus().isOk
        assertThat(Duration.between(start, now())).isGreaterThan(appProperties.defaultDelay)
    }

    private val davidLynch = Person.of("David Lynch", LocalDate.of(1946, 1, 20))

    private val mulhollandDrive = Movie.builder()
        .title("Mulholland Drive")
        .releaseYear(2001)
        .actors(
            listOf(
                Actor.builder().person(Person.of("Naomi Watts", LocalDate.of(1968, 9, 28))).role("Betty Elms").build(),
                Actor.builder().person(Person.of("Laura Harring", LocalDate.of(1964, 3, 3))).role("Rita").build()
            )
        )
        .directors(listOf(davidLynch))
        .writers(listOf(davidLynch))
        .awards(
            listOf(
                Award.builder().name("Cannes Film Festival").year(2001).build(),
                Award.builder().name("National Society of Film Critics Award").year(2001).build()
            )
        )
        .genre(Genre.MYSTERY)
        .rating(7.9)
        .build()

    private val theStraightStory = Movie.builder()
        .title("The Straight Story")
        .releaseYear(1999)
        .actors(
            listOf(
                Actor.builder().person(Person.of("Richard Farnsworth", LocalDate.of(1920, 9, 1))).role("Alvin Straight")
                    .build(),
                Actor.builder().person(Person.of("Sissy Spacek", LocalDate.of(1949, 12, 25))).role("Rose Straight")
                    .build()
            )
        )
        .directors(listOf(davidLynch))
        .writers(
            listOf(
                Person.of("John Roach", LocalDate.of(1960, 3, 2)),
                Person.of("Mary Sweeney", LocalDate.of(1954, 4, 29))
            )
        )
        .awards(
            listOf(
                Award.builder().name("Cannes Film Festival").year(1999).build(),
                Award.builder().name("Golden Globe Award").year(2000).build()
            )
        )
        .genre(Genre.DRAMA)
        .rating(8.0)
        .build()
}