package uk.gleissner.loomwebflux.movie

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@ActiveProfiles("mongo")
@Testcontainers
internal open class MovieControllerMongoIT : MovieControllerIT() {

    companion object {

        @Container
        @ServiceConnection
        val mongo = MongoDBContainer("mongo:7")
    }
}
