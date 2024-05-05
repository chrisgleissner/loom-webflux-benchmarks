package uk.gleissner.loomwebflux.fixture

import nl.altindag.log.LogCaptor
import org.junit.jupiter.api.BeforeEach
import org.junitpioneer.jupiter.cartesian.ArgumentSets
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gleissner.loomwebflux.Approaches
import uk.gleissner.loomwebflux.controller.LoomWebFluxController

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @Autowired
    lateinit var client: WebTestClient

    protected lateinit var logCaptor: LogCaptor

    @BeforeEach
    fun createLogCaptor() {
        logCaptor = LogCaptor.forClass(LoomWebFluxController::class.java)
    }

    companion object {

        @JvmStatic
        fun approaches(): List<String> = listOf(
            Approaches.PLATFORM_TOMCAT,
            Approaches.LOOM_TOMCAT,
            Approaches.LOOM_NETTY,
            Approaches.WEBFLUX_NETTY
        )

        @JvmStatic
        fun approachesAndDelayCallDepths(): ArgumentSets {
            return ArgumentSets
                .argumentsForFirstParameter(approaches())
                .argumentsForNextParameter(0, 1, 2)
        }
    }
}
