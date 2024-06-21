package uk.gleissner.loomwebflux.movie

import org.junit.jupiter.api.Disabled
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import uk.gleissner.loomwebflux.fixture.TestcontainersFixture

@ActiveProfiles("postgres-no-cache")
@Testcontainers
internal class MovieControllerPostgresNoCacheIT : MovieControllerIT() {

    @Disabled("TODO Fix this test which fails due to SQL log assertions")
    override fun `save and delete movies`(approach: String, delayCallDepth: Int) {
    }

    companion object {

        @Container
        @ServiceConnection
        val postgres = TestcontainersFixture.postgres
    }
}
