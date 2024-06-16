package uk.gleissner.loomwebflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gleissner.loomwebflux.config.AppProperties;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = AppProperties.class)
@EnableScheduling
@EnableCaching
public class LoomWebfluxApp {

    public static void main(String[] args) {
        SpringApplication.run(LoomWebfluxApp.class, args);
    }
}
