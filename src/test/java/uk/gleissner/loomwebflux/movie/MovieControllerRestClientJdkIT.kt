package uk.gleissner.loomwebflux.movie

import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@ActiveProfiles("restclient", "restclient-jdk")
@Testcontainers
internal open class MovieControllerRestClientJdkIT : MovieControllerIT()
