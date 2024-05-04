package uk.gleissner.loomwebflux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("loom-webflux")
public record AppProperties(boolean repoReadOnly, Path jvmMetricsCsvPath) {
}
