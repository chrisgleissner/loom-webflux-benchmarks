package uk.gleissner.loomwebflux.movie

import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@ActiveProfiles("restclient", "restclient-apache5")
@Testcontainers
internal open class MovieControllerRestClientApache5IT : MovieControllerIT()
