package uk.gleissner.loomwebflux

import org.junit.jupiter.api.Test

internal class LoomWebfluxAppIntegrationTest {

    @Test
    fun `load context`() {
        LoomWebfluxApp.main(arrayOf<String>())
    }
}