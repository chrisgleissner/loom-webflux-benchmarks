package uk.gleissner.loomwebflux.time

import org.assertj.core.api.Assertions.assertThat
import org.junitpioneer.jupiter.cartesian.CartesianTest
import uk.gleissner.loomwebflux.fixture.AbstractIntegrationTest
import uk.gleissner.loomwebflux.fixture.CartesianTestApproachesAndDelayCallDepths
import uk.gleissner.loomwebflux.fixture.LogCaptorFixture.assertCorrectThreadType
import java.time.Duration
import java.time.Instant
import java.time.Instant.now


internal class TimeControllerIntegrationTest : AbstractIntegrationTest() {

    @CartesianTest
    @CartesianTestApproachesAndDelayCallDepths
    fun `get epoch millis for different request depths`(approach: String, delayCallDepth: Int) {
        val startTime = now()
        val delayInMillis = 10L
        val epochMillis =
            client.get().uri {
                it
                    .path("$approach/epoch-millis")
                    .queryParam("approach", approach)
                    .queryParam("delayInMillis", delayInMillis)
                    .queryParam("delayCallDepth", delayCallDepth)
                    .build()
            }
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long::class.java)
                .returnResult().responseBody
        assertThat(Instant.ofEpochMilli(epochMillis ?: 0)).isBetween(startTime, now().plusMillis(1))
        assertThat(Duration.between(startTime, now())).isGreaterThan(Duration.ofMillis(delayInMillis))
        logCaptor.assertCorrectThreadType(approach, expectedLogCount = delayCallDepth + 1)
    }
}