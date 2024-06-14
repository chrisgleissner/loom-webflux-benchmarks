package uk.gleissner.loomwebflux

import org.junit.jupiter.api.Test
import uk.gleissner.loomwebflux.fixture.AbstractIntegrationTest
import uk.gleissner.loomwebflux.fixture.AbstractIntegrationTest.Companion.SPRING_DATASOURCE_URL

internal class LoomWebfluxAppIntegrationTest {

    @Test
    fun `load context`() {
        try {
            System.setProperty(SPRING_DATASOURCE_URL, AbstractIntegrationTest.postgres.jdbcUrl)
            LoomWebfluxApp.main(arrayOf())
        } finally {
            LoomWebfluxApp.ctx.close()
            System.clearProperty(SPRING_DATASOURCE_URL)
        }
    }
}