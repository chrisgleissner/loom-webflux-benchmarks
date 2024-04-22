package uk.gleissner.loomwebflux.movie

import nl.altindag.log.LogCaptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.BodyInserters
import uk.gleissner.loomwebflux.controller.LoomWebFluxController.*
import uk.gleissner.loomwebflux.fixture.AbstractIntegrationTest
import uk.gleissner.loomwebflux.movie.domain.*
import uk.gleissner.loomwebflux.movie.repo.MovieRepo
import java.time.LocalDate

internal class MovieControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var movieRepo: MovieRepo

    @ParameterizedTest
    @ValueSource(strings = [LOOM_TOMCAT, LOOM_NETTY, WEBFLUX_NETTY])
    fun `find movies by director last name`(approach: String) {
        val logCaptor = logCaptor()
        val movies = movies(approach)
        assertThat(movies)
            .containsExactlyElementsOf(movieRepo.findMoviesByDirector("Allen"))
        logCaptor.assertCorrectThreadType(approach)
    }

    @ParameterizedTest
    @ValueSource(strings = [LOOM_TOMCAT, LOOM_NETTY, WEBFLUX_NETTY])
    fun `save and delete a movie`(approach: String) {
        val logCaptor = logCaptor()
        val newMovie = mulhollandDrive
        assertThat(newMovie.id).isNull()
        val directorLastName = newMovie.directors[0].lastName
        val movies = movies(approach, directorLastName)
        assertThat(movies).isEmpty()

        val savedMovies = client.post().uri("$approach/movies")
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON)
            .body(
                BodyInserters.fromValue(listOf(newMovie))
            ).exchange().expectStatus().isOk
            .expectBodyList(Movie::class.java).returnResult().responseBody

        assertThat(savedMovies).hasSize(1)
        val savedMovie = savedMovies?.get(0)!!
        assertThat(savedMovie.id).isNotNull()
        assertThat(savedMovie).usingRecursiveComparison().ignoringFields("id").isEqualTo(newMovie)

        client.delete().uri("$approach/movies/${savedMovie.id}")
            .exchange().expectStatus().isOk
        assertThat(movies(approach, directorLastName)).doesNotContain(newMovie)
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

    private fun movies(approach: String, directorLastName: String = "Allen"): MutableList<Movie>? {
        val movies = client.get().uri("$approach/movies?directorLastName=$directorLastName").exchange()
            .expectStatus().isOk()
            .expectBodyList(Movie::class.java).returnResult().responseBody
        return movies
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
}