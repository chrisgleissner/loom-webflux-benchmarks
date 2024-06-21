package uk.gleissner.loomwebflux.movie

import org.junit.jupiter.api.Disabled
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@ActiveProfiles("mongo")
@Testcontainers
@Disabled("TODO cg Fix this")
internal open class MovieControllerMongoIT : MovieControllerIT() {

    companion object {

        @Container
        @ServiceConnection
        val mongo = MongoDBContainer("mongo:7")
    }
}
