package uk.gleissner.loomwebflux.movie

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import uk.gleissner.loomwebflux.fixture.TestcontainersFixture

@ActiveProfiles("postgres")
@Testcontainers
internal class MovieControllerPostgresIT : MovieControllerIT() {

    companion object {

        @Container
        @ServiceConnection
        val postgres = TestcontainersFixture.postgres
    }
}
