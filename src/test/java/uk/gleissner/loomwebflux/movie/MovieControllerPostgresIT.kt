package uk.gleissner.loomwebflux.movie

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@ActiveProfiles("postgres")
@Testcontainers
internal open class MovieControllerPostgresIT : MovieControllerIT() {

    companion object {

        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:latest")
    }
}
