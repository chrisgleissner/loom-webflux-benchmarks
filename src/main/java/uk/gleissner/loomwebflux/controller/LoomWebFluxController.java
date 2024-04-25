package uk.gleissner.loomwebflux.controller;

import lombok.extern.slf4j.Slf4j;
import uk.gleissner.loomwebflux.config.AppProperties;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public abstract class LoomWebFluxController {
    public static final String PLATFORM_TOMCAT = "/platform-tomcat";
    public static final String LOOM_TOMCAT = "/loom-tomcat";
    public static final String LOOM_NETTY = "/loom-netty";
    public static final String WEBFLUX_NETTY = "/webflux-netty";

    protected Duration actualDelay(Long delayInMillis, AppProperties appProperties) {
        return Optional.ofNullable(delayInMillis)
                .map(Duration::ofMillis)
                .orElse(appProperties.defaultDelay());
    }

    protected void log(String methodName) {
        if (log.isDebugEnabled()) {
            log.debug("{}: thread={}", methodName, Thread.currentThread());
        }
    }
}
