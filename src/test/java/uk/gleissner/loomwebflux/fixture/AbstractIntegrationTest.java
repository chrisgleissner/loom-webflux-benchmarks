package uk.gleissner.loomwebflux.fixture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract public class AbstractIntegrationTest {

    @Autowired
    protected WebTestClient client;
}
