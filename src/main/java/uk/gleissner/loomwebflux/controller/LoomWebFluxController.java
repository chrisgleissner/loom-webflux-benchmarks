package uk.gleissner.loomwebflux.controller;

import uk.gleissner.loomwebflux.config.AppProperties;

import java.time.Duration;
import java.util.Optional;

public interface LoomWebFluxController {
    String PLATFORM_TOMCAT = "platform-tomcat";
    String LOOM_TOMCAT = "/loom-tomcat";
    String LOOM_NETTY = "/loom-netty";
    String WEBFLUX_NETTY = "/webflux-netty";

    default Duration actualDelay(Long delayInMillis, AppProperties appProperties) {
        return Optional.ofNullable(delayInMillis)
                .map(Duration::ofMillis)
                .orElse(appProperties.defaultDelay());
    }
}
