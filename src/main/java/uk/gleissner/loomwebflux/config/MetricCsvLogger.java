package uk.gleissner.loomwebflux.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.stream.Collectors.joining;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricCsvLogger {

    static final String HEADER_ROW = "epochMillis,memUsed,memCommitted,memMax,gcCount,gcTime,platformThreadCount";

    private final MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final AppProperties appProperties;

    @Scheduled(fixedRate = 1000)
    public void logMetrics() {
        val csvPath = appProperties.jvmMetricsCsvPath();
        try {
            if (!Files.exists(csvPath)) {
                Files.createDirectories(csvPath.getParent());
                appendLine(HEADER_ROW);
            }
            appendLine(jvmMetrics().toCsv());
        } catch (IOException e) {
            log.error("Could not write to {}", csvPath, e);
        }
    }

    private void appendLine(String s) throws IOException {
        Files.writeString(appProperties.jvmMetricsCsvPath(), s + "\n", CREATE, APPEND);
    }

    JvmMetrics jvmMetrics() {
        val heapMemoryUsage = memoryMxBean.getHeapMemoryUsage();
        val gcStats = gcStats();
        return new JvmMetrics(
            System.currentTimeMillis(),
            heapMemoryUsage.getUsed(), heapMemoryUsage.getCommitted(), heapMemoryUsage.getMax(),
            gcStats.count, gcStats.time,
            threadMXBean.getThreadCount());
    }

    record JvmMetrics(long epochMillis,
                      long memUsed, long memCommitted, long memMax,
                      long gcCount, long gcTime,
                      long platformThreadCount) {

        public String toCsv() {
            return Stream.of(epochMillis, memUsed, memCommitted, memMax, gcCount, gcTime, platformThreadCount)
                .map(Object::toString)
                .collect(joining(","));
        }
    }

    record GcStats(long count, long time) {
    }

    GcStats gcStats() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
            .map(mbean -> new GcStats(mbean.getCollectionCount(), mbean.getCollectionTime()))
            .reduce((a, b) -> new GcStats(a.count + b.count, a.time + b.time))
            .orElse(new GcStats(0, 0));
    }
}
