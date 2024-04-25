package uk.gleissner.loomwebflux.fixture

import nl.altindag.log.LogCaptor
import org.assertj.core.api.Assertions.assertThat
import uk.gleissner.loomwebflux.controller.LoomWebFluxController.*

object LogCaptorFixture {
    fun LogCaptor.assertCorrectThreadType(approach: String) {
        val expectedThreadNameFragment =
            // TODO cg The PLATFORM_TOMCAT approach uses platform threads in prd, but VirtualThreads during test.
            if (approach == PLATFORM_TOMCAT || approach == LOOM_TOMCAT || approach == LOOM_NETTY) ".*?thread=VirtualThread\\[#\\d+,task-\\d+\\]\\/runnable@ForkJoinPool-\\d+-worker-\\d+.*?"
            else ".*?thread=Thread\\[#\\d+,reactor-http-epoll-\\d+,\\d+,main].*?"
        assertThat(debugLogs.filter { it.contains("thread=") }).allSatisfy {
            assertThat(it).matches(expectedThreadNameFragment)
        }
    }
}
