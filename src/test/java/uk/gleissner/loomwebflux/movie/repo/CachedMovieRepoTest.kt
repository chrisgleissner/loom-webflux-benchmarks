package uk.gleissner.loomwebflux.movie.repo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import uk.gleissner.loomwebflux.config.AppProperties
import uk.gleissner.loomwebflux.movie.domain.Directors.davidLynch
import uk.gleissner.loomwebflux.movie.domain.Movie
import uk.gleissner.loomwebflux.movie.domain.Movies.mulhollandDrive
import uk.gleissner.loomwebflux.movie.domain.Movies.theStraightStory

@ExtendWith(MockitoExtension::class)
class CachedMovieRepoTest {

    @Mock
    private lateinit var appProperties: AppProperties

    @Mock
    private lateinit var movieRepo: MovieRepo

    @Mock
    private lateinit var cache: Cache

    @Mock
    private lateinit var cacheManager: CacheManager

    private lateinit var sut: CachedMovieRepo


    @BeforeEach
    fun beforeEach() {
        `when`(cacheManager.getCache(any())).thenReturn(cache);
        sut = CachedMovieRepo(appProperties, movieRepo, cacheManager)
    }

    @Test
    fun `findByDirectorName should delegate to underlying repo`() {
        val directorName = davidLynch.lastName
        val expectedMovies = setOf(mulhollandDrive, theStraightStory)
        `when`(movieRepo.findByDirectorName(directorName)).thenReturn(expectedMovies)

        val movies = sut.findByDirectorName(directorName)

        assertThat(movies).isEqualTo(expectedMovies)
        verify(movieRepo).findByDirectorName(directorName)
    }

    @Test
    fun `save should return movie without saving when repoReadOnly is true`() {
        `when`(appProperties.repoReadOnly()).thenReturn(true)

        val movie = sut.save(mulhollandDrive)

        assertThat(movie).isSameAs(mulhollandDrive)
        verify(movieRepo, never()).save(any(Movie::class.java))
    }

    @Test
    fun `save should delegate to underlying repo when repoReadOnly is false`() {
        val movie = mulhollandDrive
        val savedMovie = mulhollandDrive

        `when`(appProperties.repoReadOnly()).thenReturn(false)
        `when`(movieRepo.save(movie)).thenReturn(savedMovie)

        val result = sut.save(movie)

        assertThat(result).isEqualTo(savedMovie)
        verify(movieRepo).save(movie)
    }

    @Test
    fun `deleteById should not delete when repoReadOnly is true`() {
        val movieId = 1L
        `when`(appProperties.repoReadOnly()).thenReturn(true)

        sut.deleteById(movieId)

        verify(movieRepo, never()).deleteById(movieId)
    }

    @Test
    fun `deleteById should delegate to underlying repo when repoReadOnly is false`() {
        val movieId = 1L
        `when`(appProperties.repoReadOnly()).thenReturn(false)

        sut.deleteById(movieId)

        verify(movieRepo).deleteById(movieId)
    }
}
