package uk.gleissner.loomwebflux.movie

import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import uk.gleissner.loomwebflux.config.Profiles.REST_CLIENT_JDK

@ActiveProfiles(REST_CLIENT_JDK)
@Testcontainers
internal open class MovieControllerRestClientJdkIT : MovieControllerIT()
