package uk.gleissner.loomwebflux.movie

import org.assertj.core.api.Assertions.assertThat
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.BodyInserters
import uk.gleissner.loomwebflux.fixture.AbstractIntegrationTest
import uk.gleissner.loomwebflux.fixture.CartesianTestApproachesAndDelayCallDepths
import uk.gleissner.loomwebflux.fixture.LogCaptorFixture.assertCorrectThreadType
import uk.gleissner.loomwebflux.movie.domain.*
import uk.gleissner.loomwebflux.movie.repo.MovieRepo
import java.time.Duration
import java.time.Instant.now
import java.time.LocalDate
import java.util.*

internal class MovieControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var movieRepo: MovieRepo

    private val delayInMillis = 5L

    @CartesianTest
    @CartesianTestApproachesAndDelayCallDepths
    fun `find movies by director last name`(approach: String, delayCallDepth: Int) {
        val movies = getMovies(approach, delayCallDepth = delayCallDepth)
        assertThat(movies).containsExactlyElementsOf(movieRepo.findMoviesByDirector("Allen"))
        logCaptor.assertCorrectThreadType(approach, delayCallDepth + 1)
    }

    @CartesianTest
    @CartesianTestApproachesAndDelayCallDepths
    fun `save and delete a movie`(approach: String, delayCallDepth: Int) {
        val moviesByDavidLynch = listOf(mulhollandDrive, theStraightStory)
        moviesByDavidLynch.forEach {
            assertThat(it.id).isNull()
        }

        val movies = getMovies(approach, directorLastName = davidLynch.lastName, delayCallDepth = delayCallDepth)
        assertThat(movies).isEmpty()

        val savedMovies = saveMovies(approach, moviesByDavidLynch, delayCallDepth = delayCallDepth)
        assertThat(savedMovies).hasSize(moviesByDavidLynch.size)
        savedMovies.forEach {
            assertThat(it.id).isNotNull()
        }
        assertThat(savedMovies).usingRecursiveComparison().ignoringFields("id").isEqualTo(moviesByDavidLynch)

        savedMovies.forEach {
            deleteMovie(approach, movieId = it.id, delayCallDepth = delayCallDepth)
        }
        assertThat(
            getMovies(
                approach,
                directorLastName = davidLynch.lastName,
                delayCallDepth = delayCallDepth
            )
        ).doesNotContainAnyElementsOf(savedMovies)
        logCaptor.assertCorrectThreadType(approach, expectedLogCount = (delayCallDepth + 1) * 5)
    }

    private fun getMovies(
        approach: String,
        directorLastName: String = "Allen",
        delayCallDepth: Int = 1
    ): MutableList<Movie>? {
        val startTime = now()
        val movies =
            client.get().uri {
                it.path("$approach/movies")
                    .queryParam("directorLastName", directorLastName)
                    .queryParam("approach", approach)
                    .queryParam("delayInMillis", delayInMillis)
                    .queryParam("delayCallDepth", delayCallDepth)
                    .build()
            }.exchange()
                .expectStatus().isOk()
                .expectBodyList(Movie::class.java).returnResult().responseBody
        assertThat(Duration.between(startTime, now())).isGreaterThan(Duration.ofMillis(delayInMillis))
        return movies
    }

    private fun saveMovies(approach: String, movies: List<Movie>, delayCallDepth: Int = 1): List<Movie> {
        val startTime = now()
        val savedMovies =
            client.post().uri {
                it
                    .path("$approach/movies")
                    .queryParam("approach", approach)
                    .queryParam("delayInMillis", delayInMillis)
                    .queryParam("delayCallDepth", delayCallDepth)
                    .build()
            }
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(BodyInserters.fromValue(movies)).exchange()
                .expectStatus().isOk
                .expectBodyList(Movie::class.java).returnResult().responseBody?.toList() ?: listOf()
        assertThat(Duration.between(startTime, now())).isGreaterThan(Duration.ofMillis(delayInMillis))
        return savedMovies
    }

    private fun deleteMovie(approach: String, movieId: UUID, delayCallDepth: Int = 1) {
        val startTime = now()
        client.delete().uri {
            it
                .path("$approach/movies/$movieId")
                .queryParam("approach", approach)
                .queryParam("delayInMillis", delayInMillis)
                .queryParam("delayCallDepth", delayCallDepth)
                .build()
        }.exchange().expectStatus().isOk
        assertThat(Duration.between(startTime, now())).isGreaterThan(Duration.ofMillis(delayInMillis))
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