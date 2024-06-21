package uk.gleissner.loomwebflux.time

import nl.altindag.log.LogCaptor
import org.assertj.core.api.Assertions.assertThat
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gleissner.loomwebflux.common.AbstractService
import uk.gleissner.loomwebflux.fixture.AbstractIT
import uk.gleissner.loomwebflux.fixture.CartesianTestApproachesAndDelayCallDepths
import uk.gleissner.loomwebflux.fixture.LogCaptorFixture.assertCorrectThreadType
import java.time.Duration
import java.time.Instant
import java.time.Instant.now


internal class TimeIT : AbstractIT() {

    @Autowired
    lateinit var client: WebTestClient

    @CartesianTest
    @CartesianTestApproachesAndDelayCallDepths
    fun `get epoch millis for different request depths`(approach: String, delayCallDepth: Int) {
        val logCaptor = LogCaptor.forClass(AbstractService::class.java)
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