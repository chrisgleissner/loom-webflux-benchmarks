package uk.gleissner.loomwebflux.movie.repo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import uk.gleissner.loomwebflux.config.AppProperties
import uk.gleissner.loomwebflux.movie.domain.Directors.davidLynch
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
        whenever(cacheManager.getCache(any())).thenReturn(cache)
        sut = CachedMovieRepo(appProperties, movieRepo, cacheManager)
    }

    @Test
    fun `findByDirectorName should delegate to underlying repo`() {
        val directorName = davidLynch.lastName
        val expectedMovies = setOf(mulhollandDrive, theStraightStory)
        whenever(movieRepo.findByDirectorName(directorName)).thenReturn(expectedMovies)

        val movies = sut.findByDirectorName(directorName)

        assertThat(movies).isEqualTo(expectedMovies)
        verify(movieRepo).findByDirectorName(directorName)
    }

    @Test
    fun `save should return movie without saving when repoReadOnly is true`() {
        whenever(appProperties.repoReadOnly()).thenReturn(true)

        val savedMovies = sut.saveAll(listOf(mulhollandDrive))

        assertThat(savedMovies).containsExactly(mulhollandDrive)
        verify(movieRepo, never()).saveAll(anyList())
    }

    @Test
    fun `save should delegate to underlying repo when repoReadOnly is false`() {
        val movies = listOf(mulhollandDrive)
        whenever(appProperties.repoReadOnly()).thenReturn(false)
        whenever(movieRepo.saveAll(anyIterable())).thenReturn(movies)

        val savedMovies = sut.saveAll(movies)

        assertThat(savedMovies).isEqualTo(movies)
        verify(movieRepo).saveAll(movies)
    }

    @Test
    fun `deleteById should not delete when repoReadOnly is true`() {
        val movieId = 1L
        whenever(appProperties.repoReadOnly()).thenReturn(true)

        sut.deleteById(movieId)

        verify(movieRepo, never()).deleteById(movieId)
    }

    @Test
    fun `deleteById should delegate to underlying repo when repoReadOnly is false`() {
        val movieId = 1L
        whenever(appProperties.repoReadOnly()).thenReturn(false)

        sut.deleteById(movieId)

        verify(movieRepo).deleteById(movieId)
    }
}
