package uk.gleissner.loomwebflux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties("loom-webflux")
public record AppProperties(boolean repoReadOnly, Path jvmMetricsCsvPath, WebClient webClient) {
    public record WebClient(int maxConnections, int pendingAcquireMaxCount, Duration connectTimeout, Duration pendingAcquireTimeout, Duration responseTimeout) {
    }
}
