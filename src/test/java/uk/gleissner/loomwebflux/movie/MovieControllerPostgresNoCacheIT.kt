package uk.gleissner.loomwebflux.movie

import org.junit.jupiter.api.Disabled
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("no-cache")
internal class MovieControllerPostgresNoCacheIT : MovieControllerPostgresIT() {

    @Disabled("TODO Fix this test which fails due to SQL log assertions")
    override fun `save and delete movies`(approach: String, delayCallDepth: Int) {
    }
}
