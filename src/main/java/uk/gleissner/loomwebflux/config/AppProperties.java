package uk.gleissner.loomwebflux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties("loom-webflux")
public record AppProperties(Duration defaultDelay, boolean repoReadOnly, Path jvmMetricsCsvPath) {
}
