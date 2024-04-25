package uk.gleissner.loomwebflux.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import uk.gleissner.loomwebflux.fixture.AbstractIntegrationTest
import uk.gleissner.loomwebflux.fixture.ApproachesMethodSource
import uk.gleissner.loomwebflux.fixture.LogCaptorFixture.assertCorrectThreadType
import java.time.Instant
import java.time.Instant.now

internal class TimeControllerIntegrationTest : AbstractIntegrationTest() {

    @ParameterizedTest
    @ApproachesMethodSource
    fun `get current time`(approach: String) {
        val startTime = now()
        val epochMillis = client.get().uri("$approach/epoch-millis").exchange()
            .expectStatus().isOk()
            .expectBody(Long::class.java)
            .returnResult().responseBody
        assertThat(Instant.ofEpochMilli(epochMillis ?: 0)).isBetween(startTime, now().plusMillis(1));
        logCaptor.assertCorrectThreadType(approach)
    }
}