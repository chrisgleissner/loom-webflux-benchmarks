package uk.gleissner.loomwebflux.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import uk.gleissner.loomwebflux.config.AppProperties.WebClient
import uk.gleissner.loomwebflux.config.MetricCsvLogger.HEADER_ROW
import uk.gleissner.loomwebflux.config.MetricCsvLogger.JvmMetrics
import java.nio.file.Files
import java.nio.file.Path

internal class MetricCsvLoggerTest {
    private val csvPath = Path.of("build/test-output/${javaClass.simpleName}/jvm.csv")
    private val csvRowRegex = "[\\d+,]+\n"
    private val sut = MetricCsvLogger(AppProperties(false, csvPath, mock(WebClient::class.java)))

    @Test
    fun `Given no CSV file When metrics are logged Then a header is written to the metric file And the metrics are appended`() {
        Files.deleteIfExists(csvPath)
        assertThat(csvPath).doesNotExist()

        sut.logMetrics()
        assertThat(csvPath).isRegularFile().content().matches("$HEADER_ROW\n$csvRowRegex")

        sut.logMetrics()
        assertThat(csvPath).isRegularFile().content().matches("$HEADER_ROW\n$csvRowRegex$csvRowRegex")
    }

    @Test
    fun `When the jvmMetrics is called Then JVM metrics are returned`() {
        val startTime = System.currentTimeMillis();
        val metrics = sut.jvmMetrics()

        assertThat(metrics.epochMillis).isBetween(startTime, System.currentTimeMillis() + 1)

        assertThat(metrics.memUsed).isGreaterThan(0).isLessThanOrEqualTo(metrics.memCommitted)
            .isLessThanOrEqualTo(metrics.memMax)
        assertThat(metrics.memCommitted).isLessThanOrEqualTo(metrics.memMax)

        assertThat(metrics.platformThreadCount).isPositive()

        System.gc()

        val metricsPostGc = sut.jvmMetrics()
        assertThat(metricsPostGc.gcCount).isGreaterThan(metrics.gcCount)
        assertThat(metricsPostGc.gcTime).isGreaterThanOrEqualTo(metrics.gcTime)
    }

    @Test
    fun `When the toCsv method is called Then a CSV representation of the JvmMetrics is returned`() {
        assertThat(JvmMetrics(1, 2, 3, 4, 5, 6, 7).toCsv()).isEqualTo("1,2,3,4,5,6,7")
    }
}