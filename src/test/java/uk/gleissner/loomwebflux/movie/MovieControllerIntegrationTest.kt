package uk.gleissner.loomwebflux.movie

import nl.altindag.log.LogCaptor
import org.assertj.core.api.Assertions.assertThat
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.BodyInserters
import uk.gleissner.loomwebflux.fixture.AbstractIntegrationTest
import uk.gleissner.loomwebflux.fixture.CartesianTestApproachesAndDelayCallDepths
import uk.gleissner.loomwebflux.fixture.LogCaptorFixture.assertCorrectThreadType
import uk.gleissner.loomwebflux.movie.domain.Directors.davidLynch
import uk.gleissner.loomwebflux.movie.domain.Movie
import uk.gleissner.loomwebflux.movie.domain.Movies.mulhollandDrive
import uk.gleissner.loomwebflux.movie.domain.Movies.theStraightStory
import uk.gleissner.loomwebflux.movie.repo.MovieRepo
import java.time.Duration
import java.time.Instant.now

private const val HIBERNATE_ORM_CACHE_LOG_NAME = "org.hibernate.orm.cache"
private const val RETURNING_CACHED_QUERY_RESULTS = "Returning cached query results"
private const val CACHED_QUERY_RESULTS_WERE_NOT_UP_TO_DATE = "Cached query results were not up-to-date"

internal class MovieControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var movieRepo: MovieRepo

    private val delayInMillis = 5L

    @CartesianTest
    @CartesianTestApproachesAndDelayCallDepths
    fun `find movies by director last name`(approach: String, delayCallDepth: Int) {
        val movies = getMovies(approach, delayCallDepth = delayCallDepth)
        assertThat(movies).containsExactlyElementsOf(movieRepo.findByDirectorName("Allen"))
        logCaptor.assertCorrectThreadType(approach, delayCallDepth + 1)
    }

    @CartesianTest
    @CartesianTestApproachesAndDelayCallDepths
    fun `save and delete movies`(approach: String, delayCallDepth: Int) {
        val movies = listOf(mulhollandDrive, theStraightStory)
        fun getMovies() = getMovies(approach, directorLastName = davidLynch.lastName, delayCallDepth = delayCallDepth)
        assertThat(getMovies()).isEmpty()

        val savedMovies = saveMovies(approach, movies, delayCallDepth = delayCallDepth)
        assertThat(savedMovies).hasSize(movies.size)
        savedMovies.forEach {
            assertThat(it.id).isNotNull()
        }
        assertThat(savedMovies).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*id").isEqualTo(movies)

        secondLevelCacheLogCaptor().use { logCaptor ->
            assertThat(getMovies()).containsExactlyElementsOf(savedMovies)
            logCaptor.assertThatCached(false)
        }

        secondLevelCacheLogCaptor().use { logCaptor ->
            assertThat(getMovies()).containsExactlyElementsOf(savedMovies)
            logCaptor.assertThatCached(true)
        }

        secondLevelCacheLogCaptor().use { logCaptor ->
            savedMovies.forEach { savedMovie ->
                deleteMovie(approach, movieId = savedMovie.id, delayCallDepth = delayCallDepth)
            }
            assertThat(getMovies()).isEmpty()
            logCaptor.assertThatCached(false)
        }

        logCaptor.assertCorrectThreadType(approach, expectedLogCount = (delayCallDepth + 1) * 7)
    }

    private fun secondLevelCacheLogCaptor() = LogCaptor.forName(HIBERNATE_ORM_CACHE_LOG_NAME)

    private fun LogCaptor.assertThatCached(cached: Boolean) {
        if (cached) {
            assertThat(debugLogs).contains(RETURNING_CACHED_QUERY_RESULTS).doesNotContain(CACHED_QUERY_RESULTS_WERE_NOT_UP_TO_DATE)
        } else {
            assertThat(debugLogs).doesNotContain(RETURNING_CACHED_QUERY_RESULTS).contains(CACHED_QUERY_RESULTS_WERE_NOT_UP_TO_DATE)
        }
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
                    .queryParam("delayInMillis", delayInMillis)
                    .queryParam("delayCallDepth", delayCallDepth)
                    .build()
            }.exchange()
                .expectStatus().isOk()
                .expectBodyList(Movie::class.java)
                .returnResult()
                .responseBody
        assertThat(Duration.between(startTime, now())).isGreaterThan(Duration.ofMillis(delayInMillis))
        return movies
    }

    private fun saveMovies(approach: String, movies: List<Movie>, delayCallDepth: Int = 1): List<Movie> {
        val startTime = now()
        val savedMovies =
            client.post().uri {
                it
                    .path("$approach/movies")
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

    private fun deleteMovie(approach: String, movieId: Long, delayCallDepth: Int = 1) {
        val startTime = now()
        client.delete().uri {
            it
                .path("$approach/movies/$movieId")
                .queryParam("delayInMillis", delayInMillis)
                .queryParam("delayCallDepth", delayCallDepth)
                .build()
        }.exchange().expectStatus().isOk
        assertThat(Duration.between(startTime, now())).isGreaterThan(Duration.ofMillis(delayInMillis))
    }
}