package uk.gleissner.loomwebflux.movie.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import uk.gleissner.loomwebflux.movie.domain.Genre.ROMANCE
import java.time.LocalDate


@JsonTest
class MovieJsonTest(@Autowired val jacksonTester: JacksonTester<List<Movie>>) {

    @Test
    fun `can deserialize movies json`() {
        val movies: List<Movie> = jacksonTester.read("/scenarios/movies.json").`object`

        assertThat(movies).hasSize(8)

        val annieHall = movies[0]
        assertThat(annieHall.title).isEqualTo("Annie Hall")
        assertThat(annieHall.releaseYear).isEqualTo(1977)
        assertThat(annieHall.genre).isEqualTo(ROMANCE)

        assertThat(annieHall.characters).hasSize(2)
        assertThat(annieHall.characters[0].name).isEqualTo("Alvy Singer")

        val woodyAllen = Person(null, "Woody", "Allen", LocalDate.of(1935, 11, 30))
        assertThat(annieHall.characters[0].actor).isEqualTo(woodyAllen)
        assertThat(annieHall.directors).containsExactly(woodyAllen)
        assertThat(annieHall.writers).containsExactly(woodyAllen)

        assertThat(annieHall.awards).hasSize(2)
        assertThat(annieHall.awards[0].name).isEqualTo("Academy Award")
        assertThat(annieHall.awards[0].year).isEqualTo(1978)

        assertThat(annieHall.rating).isEqualTo(8.0)
    }
}