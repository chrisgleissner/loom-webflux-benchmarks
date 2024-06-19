package uk.gleissner.loomwebflux.fixture

import nl.altindag.log.LogCaptor
import org.junit.jupiter.api.BeforeEach
import org.junitpioneer.jupiter.cartesian.ArgumentSets
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import uk.gleissner.loomwebflux.common.AbstractService
import uk.gleissner.loomwebflux.common.Approaches.*


@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @Autowired
    lateinit var client: WebTestClient

    protected lateinit var logCaptor: LogCaptor

    @BeforeEach
    fun createLogCaptor() {
        logCaptor = LogCaptor.forClass(AbstractService::class.java)
    }

    companion object {

        // Set to true for PostgreSQL test
        private const val postgreSqlEnabled = false

        @JvmStatic
        @DynamicPropertySource
        fun startPostgresql(registry: DynamicPropertyRegistry) {
            if (postgreSqlEnabled) {
                val username = "postgres"
                val password = "password"
                val postgresql = PostgreSQLContainer("postgres:16-alpine").withUsername(username).withPassword(password)
                postgresql.start()
                registry.add("spring.datasource.url", postgresql::getJdbcUrl)
                registry.add("spring.datasource.username") { username }
                registry.add("spring.datasource.password") { password }
            }
        }

        @JvmStatic
        fun approaches(): List<String> = listOf(PLATFORM_TOMCAT, LOOM_TOMCAT, LOOM_NETTY, WEBFLUX_NETTY)

        @JvmStatic
        fun approachesAndDelayCallDepths(): ArgumentSets {
            return ArgumentSets
                .argumentsForFirstParameter(approaches())
                .argumentsForNextParameter(0, 1, 2)
        }
    }
}
