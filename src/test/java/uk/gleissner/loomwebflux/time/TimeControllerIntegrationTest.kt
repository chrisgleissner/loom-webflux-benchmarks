package uk.gleissner.loomwebflux.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gleissner.loomwebflux.controller.LoomWebFluxController.LOOM_NETTY
import uk.gleissner.loomwebflux.controller.LoomWebFluxController.LOOM_TOMCAT
import uk.gleissner.loomwebflux.fixture.AbstractIntegrationTest
import java.time.Instant

internal class TimeControllerIntegrationTest : AbstractIntegrationTest() {

    @ParameterizedTest
    @ValueSource(strings = [LOOM_TOMCAT, LOOM_NETTY])
    fun `get current time`(approach: String) {
        val startTime = Instant.now()
        val epochMillis = client.get().uri("$approach/epoch-millis").exchange()
            .expectStatus().isOk()
            .expectBody(Long::class.java)
            .returnResult().responseBody
        assertThat(Instant.ofEpochMilli(epochMillis ?: 0)).isBetween(startTime, Instant.now().plusMillis(1));
    }

}